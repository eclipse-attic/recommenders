/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Sebastian Proksch - initial API and implementation
 */
package org.eclipse.recommenders.internal.extdoc.rcp.wiring;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.recommenders.extdoc.rcp.providers.ExtdocProvider;
import org.eclipse.recommenders.extdoc.rcp.providers.ExtdocProviderDescription;
import org.eclipse.recommenders.internal.extdoc.rcp.ui.ExtdocPreferences;
import org.eclipse.recommenders.internal.extdoc.rcp.wiring.ManualModelStoreWiring.ClassOverridesModelStore;
import org.eclipse.recommenders.internal.extdoc.rcp.wiring.ManualModelStoreWiring.ClassOverridesPatternsModelStore;
import org.eclipse.recommenders.internal.extdoc.rcp.wiring.ManualModelStoreWiring.ClassSelfcallsModelStore;
import org.eclipse.recommenders.internal.extdoc.rcp.wiring.ManualModelStoreWiring.MethodSelfcallsModelStore;
import org.eclipse.recommenders.rcp.RecommendersPlugin;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

public class ExtdocModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ExtdocPreferences.class).in(Scopes.SINGLETON);
        bind(ClassOverridesPatternsModelStore.class).in(Scopes.SINGLETON);
        bind(ClassOverridesModelStore.class).in(Scopes.SINGLETON);
        bind(ClassSelfcallsModelStore.class).in(Scopes.SINGLETON);
        bind(MethodSelfcallsModelStore.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    @Extdoc
    IPreferenceStore providePreferenceStore() {
        return ExtdocPlugin.getDefault().getPreferenceStore();
    }

    @Provides
    @Singleton
    List<ExtdocProvider> provideProviders() {
        final List<ExtdocProvider> providers = instantiateProvidersFromRegistry();
        return providers;
    }

    static List<ExtdocProvider> instantiateProvidersFromRegistry() {
        final IConfigurationElement[] elements =
                Platform.getExtensionRegistry()
                        .getConfigurationElementsFor("org.eclipse.recommenders.extdoc.rcp.provider");
        final List<ExtdocProvider> providers = Lists.newLinkedList();

        for (final IConfigurationElement element : elements) {
            final Optional<ExtdocProvider> opt = createProvider(element);
            if (opt.isPresent()) {
                providers.add(opt.get());
            }
        }

        Collections.sort(providers, new Comparator<ExtdocProvider>() {

            @Override
            public int compare(ExtdocProvider o1, ExtdocProvider o2) {
                String n1 = o1.getDescription().getName();
                String n2 = o2.getDescription().getName();
                if (n1.equals("Javadoc")) {
                    return -1;
                } else if (n2.equals("Javadoc")) {
                    return 1;
                } else {
                    return n1.compareTo(n2);
                }
            }
        });
        return providers;
    }

    static Optional<ExtdocProvider> createProvider(final IConfigurationElement element) {
        final String pluginId = element.getContributor().getName();
        try {
            final String imagePath = element.getAttribute("image");
            final String name = element.getAttribute("name");
            final Image image = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, imagePath).createImage();
            final ExtdocProvider provider = (ExtdocProvider) element.createExecutableExtension("class");
            final ExtdocProviderDescription description = new ExtdocProviderDescription(name, image);
            provider.setDescription(description);
            return Optional.of(provider);
        } catch (final Exception e) {
            RecommendersPlugin.logError(e,
                    "failed to instantiate provider %s:%s",
                    pluginId,
                    element.getAttribute("class"));
            return Optional.absent();
        }
    }

    @BindingAnnotation
    @Target({ METHOD, PARAMETER })
    @Retention(RUNTIME)
    public static @interface Extdoc {
    }
}