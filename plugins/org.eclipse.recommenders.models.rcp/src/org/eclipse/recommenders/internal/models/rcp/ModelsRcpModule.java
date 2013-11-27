/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olav Lenz - initial API and implementation
 */
package org.eclipse.recommenders.internal.models.rcp;

import static com.google.inject.Scopes.SINGLETON;

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.recommenders.models.IModelArchiveCoordinateAdvisor;
import org.eclipse.recommenders.models.IModelIndex;
import org.eclipse.recommenders.models.IModelRepository;
import org.eclipse.recommenders.models.IProjectCoordinateAdvisorService;
import org.eclipse.recommenders.models.advisors.ModelIndexBundleSymbolicNameAdvisor;
import org.eclipse.recommenders.models.advisors.ModelIndexFingerprintAdvisor;
import org.eclipse.recommenders.models.advisors.SharedManualMappingsAdvisor;
import org.eclipse.recommenders.models.rcp.IProjectCoordinateProvider;
import org.eclipse.ui.IWorkbench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Names;

@SuppressWarnings("restriction")
public class ModelsRcpModule extends AbstractModule implements Module {

    public static final String IDENTIFIED_PROJECT_COORDINATES = "IDENTIFIED_PACKAGE_FRAGMENT_ROOTS";
    public static final String REPOSITORY_BASEDIR = "REPOSITORY_BASEDIR";
    public static final String INDEX_BASEDIR = "INDEX_BASEDIR";
    public static final String MANUAL_MAPPINGS = "MANUAL_MAPPINGS";
    public static final String AVAILABLE_ADVISORS = "DEFAULT_ADVISORS";

    private static final Logger LOG = LoggerFactory.getLogger(ModelsRcpModule.class);

    @Override
    protected void configure() {
        requestStaticInjection(Dependencies.class);
        //
        bind(IProjectCoordinateProvider.class).to(ProjectCoordinateProvider.class).in(SINGLETON);
        bind(IProjectCoordinateAdvisorService.class).to(EclipseProjectCoordinateAdvisorService.class).in(SINGLETON);

        // bind all clients of IRecommendersModelIndex or its super interface IModelArchiveCoordinateProvider to a
        // single instance in Eclipse:
        bind(EclipseModelIndex.class).in(SINGLETON);
        bind(IModelArchiveCoordinateAdvisor.class).to(EclipseModelIndex.class);
        bind(IModelIndex.class).to(EclipseModelIndex.class);
        createAndBindNamedFile("index", INDEX_BASEDIR);

        //
        bind(EclipseModelRepository.class).in(SINGLETON);
        bind(IModelRepository.class).to(EclipseModelRepository.class);
        createAndBindNamedFile("repository", REPOSITORY_BASEDIR);

        // configure caching
        bind(ManualProjectCoordinateAdvisor.class).in(SINGLETON);
        createAndBindNamedFile("caches/manual-mappings.json", MANUAL_MAPPINGS);
        createAndBindNamedFile("caches/identified-project-coordinates.json", IDENTIFIED_PROJECT_COORDINATES);

    }

    private void createAndBindNamedFile(String fileName, String name) {
        File rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
        File stateLocation = new File(rootLocation, ".recommenders");
        File file = new File(stateLocation, fileName);
        try {
            Files.createParentDirs(file);
        } catch (IOException e) {
            LOG.error("failed to bind file name", e);
        }
        bind(File.class).annotatedWith(Names.named(name)).toInstance(file);
    }

    @Singleton
    @Provides
    public EclipseDependencyListener provideMappingProvider(EventBus bus) {
        return new EclipseDependencyListener(bus);
    }

    @Provides
    public IProxyService provideProxyService() {
        return ProxyManager.getProxyManager();
    }

    @Provides
    public ModelIndexBundleSymbolicNameAdvisor provideModelIndexBundleSymbolicNameAdvisor(IModelIndex index) {
        return new ModelIndexBundleSymbolicNameAdvisor(index);
    }

    @Provides
    public ModelIndexFingerprintAdvisor provideModelIndexFingerprintAdvisor(IModelIndex index) {
        return new ModelIndexFingerprintAdvisor(index);
    }

    @Provides
    public SharedManualMappingsAdvisor provideWorkspaceMappingsAdvisor(IModelRepository repository) {
        return new SharedManualMappingsAdvisor(repository);
    }

    @Provides
    @Singleton
    public ModelsRcpPreferences provide(IWorkbench wb) {
        IEclipseContext context = (IEclipseContext) wb.getService(IEclipseContext.class);
        ModelsRcpPreferences prefs = ContextInjectionFactory.make(ModelsRcpPreferences.class, context);
        return prefs;
    }
}
