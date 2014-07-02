/**
 * Copyright (c) 2010 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.injection;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Module;

public class InjectionDescriptor {

    private static final Logger LOG = LoggerFactory.getLogger(InjectionDescriptor.class);
    private static final String ATTR_CLASS = "class";
    private static final String EXT_POINT_ID = "org.eclipse.recommenders.injection.modules";

    public static List<InjectionDescriptor> getDescriptors() {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint extpoint = registry.getExtensionPoint(EXT_POINT_ID);
        final List<InjectionDescriptor> descriptors = Lists.newArrayList();
        for (final IExtension ext : extpoint.getExtensions()) {
            for (final IConfigurationElement config : ext.getConfigurationElements()) {
                final InjectionDescriptor desc = new InjectionDescriptor(config);
                descriptors.add(desc);
            }
        }
        return descriptors;
    }

    public static List<Module> createModules() {
        final List<Module> engines = Lists.newArrayList();
        for (final InjectionDescriptor desc : getDescriptors()) {
            try {
                engines.add(desc.createInstance());
            } catch (final CoreException e) {
                LOG.error("Failed to create module from " + desc.getPluginId(), e);
            }
        }
        return engines;
    }

    private final IConfigurationElement element;

    protected InjectionDescriptor(final IConfigurationElement element) {
        this.element = element;
    }

    public String getPluginId() {
        return element.getContributor().getName();
    }

    public Module createInstance() throws CoreException {
        return (Module) element.createExecutableExtension(ATTR_CLASS);
    }
}
