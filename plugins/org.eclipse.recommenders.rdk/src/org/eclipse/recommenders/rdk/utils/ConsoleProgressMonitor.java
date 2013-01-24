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
package org.eclipse.recommenders.rdk.utils;

import org.eclipse.core.runtime.NullProgressMonitor;

public final class ConsoleProgressMonitor extends NullProgressMonitor {
    @Override
    public void subTask(String name) {
        System.out.println(name);
    }
}
