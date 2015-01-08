/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.models.rcp;

import static org.eclipse.core.runtime.IStatus.ERROR;

import org.eclipse.recommenders.utils.Logs;
import org.eclipse.recommenders.utils.Logs.DefaultLogMessage;
import org.eclipse.recommenders.utils.Logs.ILogMessage;
import org.osgi.framework.Bundle;

public final class LogMessages extends DefaultLogMessage {

    private static int code = 1;

    private static final Bundle BUNDLE = Logs.getBundle(LogMessages.class);

    public static final LogMessages SAVE_PREFERENCES_FAILED = new LogMessages(ERROR,
            Messages.LOG_ERROR_SAVE_PREFERENCES);
    public static final LogMessages ADVISOR_INSTANTIATION_FAILED = new LogMessages(ERROR,
            Messages.LOG_ERROR_ADVISOR_INSTANTIATION);
    public static ILogMessage LOG_ERROR_BIND_FILE_NAME = new LogMessages(ERROR, Messages.LOG_ERROR_BIND_FILE_NAME);
    public static final ILogMessage LOG_ERROR_CREATE_EXECUTABLE_EXTENSION_FAILED = new LogMessages(ERROR,
            Messages.LOG_ERROR_CREATE_EXECUTABLE_EXTENSION_FAILED);

    private LogMessages(int severity, String message) {
        super(severity, code++, message);
    }

    @Override
    public Bundle bundle() {
        return BUNDLE;
    }
}
