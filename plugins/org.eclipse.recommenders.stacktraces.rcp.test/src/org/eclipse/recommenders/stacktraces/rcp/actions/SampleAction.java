/**
 * Copyright (c) 2010, 2014 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.stacktraces.rcp.actions;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.osgi.framework.FrameworkUtil;

public class SampleAction implements IWorkbenchWindowActionDelegate {

    private static final String component = Character.toString((char) 31);
    private static final String data = Character.toString((char) 29);
    private static final String segment = Character.toString((char) 28);

    @Override
    public void run(IAction action) {
        ILog log = Platform.getLog(FrameworkUtil.getBundle(getClass()));
        RuntimeException cause = new RuntimeException("cause");
        cause.fillInStackTrace();
        String invis = "sep:" + component + " data: " + data + " segment: " + segment;
        Exception exception = new RuntimeException("exception message: component" + invis, cause);
        exception.fillInStackTrace();
        log.log(new Status(IStatus.ERROR, "org.eclipse.recommenders.stacktraces", "some error message: " + invis,
                exception));
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void init(IWorkbenchWindow window) {
    }

}
