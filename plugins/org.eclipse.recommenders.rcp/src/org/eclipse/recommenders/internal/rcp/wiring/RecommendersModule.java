/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.rcp.wiring;

import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.eclipse.recommenders.rcp.RecommendersPlugin.P_REPOSITORY_URL;
import static org.eclipse.recommenders.utils.Executors.coreThreadsTimoutExecutor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.recommenders.internal.rcp.providers.CachingAstProvider;
import org.eclipse.recommenders.internal.rcp.providers.JavaModelEventsProvider;
import org.eclipse.recommenders.internal.rcp.providers.JavaSelectionProvider;
import org.eclipse.recommenders.rcp.IAstProvider;
import org.eclipse.recommenders.rcp.RecommendersPlugin;
import org.eclipse.recommenders.utils.rcp.JavaElementResolver;
import org.eclipse.recommenders.utils.rcp.ast.ASTNodeUtils;
import org.eclipse.recommenders.utils.rcp.ast.ASTStringUtils;
import org.eclipse.recommenders.utils.rcp.ast.BindingUtils;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

@SuppressWarnings("restriction")
public class RecommendersModule extends AbstractModule implements Module {

    @Override
    protected void configure() {
        configureJavaElementResolver();
        configureAstProvider();
        bindRepository();
        bindShutdownListener();
        bindServiceListener();
    }

    private void configureJavaElementResolver() {
        bind(JavaElementResolver.class).in(Scopes.SINGLETON);
        requestStaticInjection(ASTStringUtils.class);
        requestStaticInjection(ASTNodeUtils.class);
        requestStaticInjection(BindingUtils.class);
    }

    private void configureAstProvider() {
        final CachingAstProvider p = new CachingAstProvider();
        JavaCore.addElementChangedListener(p);
        bind(IAstProvider.class).toInstance(p);
    }

    private void bindRepository() {

        Bundle bundle = FrameworkUtil.getBundle(getClass());
        File stateLocation = Platform.getStateLocation(bundle).toFile();

        File repo = new File(stateLocation, "repository"); //$NON-NLS-1$
        repo.mkdirs();
        RecommendersPlugin plugin = RecommendersPlugin.getDefault();
        IPreferenceStore store = plugin.getPreferenceStore();
        String url = store.getString(P_REPOSITORY_URL);
        bind(File.class).annotatedWith(LocalModelRepositoryLocation.class).toInstance(repo);
        bind(String.class).annotatedWith(RemoteModelRepositoryLocation.class).toInstance(url
        // "file:/Volumes/usb/juno/m2/"
        //
                );

        File index = new File(stateLocation, "index"); //$NON-NLS-1$
        index.mkdirs();
        bind(File.class).annotatedWith(ModelRepositoryIndexLocation.class).toInstance(index);
    }

    private void bindShutdownListener() {
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
                typeEncounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(final I i) {
                        if (i instanceof Closeable
                                && i.getClass().isAnnotationPresent(AutoCloseOnWorkbenchShutdown.class)) {
                            PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {

                                @Override
                                public boolean preShutdown(IWorkbench workbench, boolean forced) {
                                    try {
                                        ((Closeable) i).close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    return true;
                                }

                                @Override
                                public void postShutdown(IWorkbench workbench) {
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void bindServiceListener() {
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
                typeEncounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(final I i) {
                        if (i instanceof Service) {
                            // start it:
                            ((Service) i).startAndWait();

                            PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {

                                @Override
                                public boolean preShutdown(IWorkbench workbench, boolean forced) {
                                    try {
                                        ((Service) i).stop().get(5, TimeUnit.SECONDS);
                                    } catch (Exception e) {
                                    }
                                    return true;
                                }

                                @Override
                                public void postShutdown(IWorkbench workbench) {
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    @BindingAnnotation
    @Target(PARAMETER)
    @Retention(RUNTIME)
    public static @interface LocalModelRepositoryLocation {
    }

    @BindingAnnotation
    @Target(ElementType.TYPE)
    @Retention(RUNTIME)
    public static @interface AutoCloseOnWorkbenchShutdown {
    }

    @BindingAnnotation
    @Target(PARAMETER)
    @Retention(RUNTIME)
    public static @interface RemoteModelRepositoryLocation {
    }

    @BindingAnnotation
    @Target(PARAMETER)
    @Retention(RUNTIME)
    public static @interface ModelRepositoryIndexLocation {
    }

    @Singleton
    @Provides
    protected JavaModelEventsProvider provideJavaModelEventsProvider(final EventBus bus, final IWorkspaceRoot workspace) {
        final JavaModelEventsProvider p = new JavaModelEventsProvider(bus, workspace);
        JavaCore.addElementChangedListener(p);
        return p;
    }

    @Singleton
    @Provides
    // @Workspace
    protected EventBus provideWorkspaceEventBus() {
        final int numberOfCores = Runtime.getRuntime().availableProcessors();
        final ExecutorService pool = coreThreadsTimoutExecutor(numberOfCores + 1, MIN_PRIORITY,
                "Recommenders-Bus-Thread-", //$NON-NLS-1$
                1L, TimeUnit.MINUTES);
        final EventBus bus = new AsyncEventBus("Code Recommenders asychronous Workspace Event Bus", pool); //$NON-NLS-1$
        return bus;
    }

    @Provides
    @Singleton
    protected JavaSelectionProvider provideJavaSelectionProvider(final EventBus bus) {
        final JavaSelectionProvider provider = new JavaSelectionProvider(bus);
        new UIJob("Registering workbench selection listener.") { //$NON-NLS-1$
            {
                schedule();
            }

            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
                final IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                final ISelectionService service = (ISelectionService) ww.getService(ISelectionService.class);
                service.addPostSelectionListener(provider);
                return Status.OK_STATUS;
            }
        };
        return provider;
    }

    @Provides
    protected IWorkspaceRoot provideWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    // @Provides
    // protected ProxySelector provideProxyService() {
    // Bundle bundle = FrameworkUtil.getBundle(getClass());
    // ServiceTracker tracker = new ServiceTracker(bundle.getBundleContext(), IProxyService.class.getName(), null);
    // tracker.open();
    // IProxyService service = (IProxyService) tracker.getService();
    // tracker.close();
    // return new ServiceBasedProxySelector(service);
    // }

    @Provides
    protected IWorkspace provideWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    @Provides
    protected Display provideDisplay() {
        Display d = Display.getCurrent();
        if (d == null) {
            d = Display.getDefault();
        }
        return d;
    }

    @Provides
    protected IWorkbench provideWorkbench() {
        return PlatformUI.getWorkbench();
    }

    @Provides
    protected IWorkbenchPage provideActiveWorkbenchPage(final IWorkbench wb) {

        if (isRunningInUiThread()) {
            return wb.getActiveWorkbenchWindow().getActivePage();
        }

        return runUiFinder().activePage;
    }

    private ActivePageFinder runUiFinder() {
        final ActivePageFinder finder = new ActivePageFinder();
        try {
            if (isRunningInUiThread()) {
                finder.call();
            } else {
                final FutureTask<IWorkbenchPage> task = new FutureTask(finder);
                Display.getDefault().asyncExec(task);
                task.get(2, TimeUnit.SECONDS);
            }
        } catch (final Exception e) {
            RecommendersPlugin.logError(e, "Could not run 'active page finder' that early!"); //$NON-NLS-1$
        }
        return finder;
    }

    private boolean isRunningInUiThread() {
        return Display.getCurrent() != null;
    }

    @Provides
    protected IJavaModel provideJavaModel() {
        return JavaModelManager.getJavaModelManager().getJavaModel();
    }

    @Provides
    protected JavaModelManager provideJavaModelManger() {
        return JavaModelManager.getJavaModelManager();
    }

    @Provides
    protected IExtensionRegistry provideRegistry() {
        return Platform.getExtensionRegistry();
    }

    private final class ActivePageFinder implements Callable<IWorkbenchPage> {
        private IWorkbench workbench;
        private IWorkbenchWindow activeWorkbenchWindow;
        private IWorkbenchPage activePage;

        @Override
        public IWorkbenchPage call() throws Exception {
            workbench = PlatformUI.getWorkbench();
            activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
            activePage = activeWorkbenchWindow.getActivePage();
            return activePage;
        }
    }
}
