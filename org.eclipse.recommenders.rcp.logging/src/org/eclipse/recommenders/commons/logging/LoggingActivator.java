/**
 * Copyright (c) 2010, 2011 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 */
package org.eclipse.recommenders.commons.logging;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class LoggingActivator implements BundleActivator {

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        SLF4JBridgeHandler.install();
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
    }

}
