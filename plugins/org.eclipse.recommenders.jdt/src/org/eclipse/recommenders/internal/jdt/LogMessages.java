/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andreas Sewe - initial API and implementation.
 */
package org.eclipse.recommenders.internal.jdt;

import static org.eclipse.core.runtime.IStatus.ERROR;

import org.eclipse.recommenders.utils.Logs;
import org.eclipse.recommenders.utils.Logs.DefaultLogMessage;
import org.eclipse.recommenders.utils.Logs.ILogMessage;
import org.osgi.framework.Bundle;

public final class LogMessages extends DefaultLogMessage {

    private static int code = 1;

    private static final Bundle BUNDLE = Logs.getBundle(LogMessages.class);

    public static final ILogMessage ERROR_SNIPPET_REPLACE_LEADING_WHITESPACE_FAILED = new LogMessages(ERROR,
            Messages.LOG_ERROR_SNIPPET_REPLACE_LEADING_WHITESPACE_FAILED);
    public static final ILogMessage ERROR_CANNOT_FETCH_JAVA_PROJECTS = new LogMessages(ERROR,
            Messages.LOG_ERROR_CANNOT_FETCH_JAVA_PROJECTS);
    public static final ILogMessage ERROR_CANNOT_FETCH_PACKAGE_FRAGMENT_ROOTS = new LogMessages(ERROR,
            Messages.LOG_ERROR_CANNOT_FETCH_PACKAGE_FRAGMENT_ROOTS);
    public static final ILogMessage ERROR_CANNOT_FETCH_PACKAGE_FRAGMENT = new LogMessages(ERROR,
            Messages.LOG_ERROR_CANNOT_FETCH_PACKAGE_FRAGMENT);
    public static final ILogMessage ERROR_CANNOT_FETCH_COMPILATION_UNITS = new LogMessages(ERROR,
            Messages.LOG_ERROR_CANNOT_FETCH_COMPILATION_UNITS);
    public static final ILogMessage ERROR_CANNOT_FIND_TYPE_IN_PROJECT = new LogMessages(ERROR,
            Messages.LOG_ERROR_CANNOT_FIND_TYPE_IN_PROJECT);

    private LogMessages(int severity, String message) {
        super(severity, code++, message);
    }

    @Override
    public Bundle bundle() {
        return BUNDLE;
    }
}
