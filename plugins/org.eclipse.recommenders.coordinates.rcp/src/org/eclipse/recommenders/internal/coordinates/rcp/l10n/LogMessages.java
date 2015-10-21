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
package org.eclipse.recommenders.internal.coordinates.rcp.l10n;

import static org.eclipse.core.runtime.IStatus.ERROR;

import org.eclipse.recommenders.utils.Logs;
import org.eclipse.recommenders.utils.Logs.DefaultLogMessage;
import org.eclipse.recommenders.utils.Logs.ILogMessage;
import org.osgi.framework.Bundle;

public final class LogMessages extends DefaultLogMessage {

    private static int code = 1;

    private static final Bundle BUNDLE = Logs.getBundle(LogMessages.class);

    public static final ILogMessage ERROR_ADVISOR_INSTANTIATION_FAILED = new LogMessages(ERROR,
            Messages.LOG_ERROR_ADVISOR_INSTANTIATION_FAILED);
    public static final ILogMessage ERROR_BIND_FILE_NAME = new LogMessages(ERROR, Messages.LOG_ERROR_BIND_FILE_NAME);
    public static final ILogMessage ERROR_FAILED_TO_CREATE_ADVISOR = new LogMessages(ERROR,
            Messages.LOG_ERROR_FAILED_TO_CREATE_ADVISOR);
    public static final ILogMessage ERROR_FAILED_TO_DETECT_PROJECT_JRE = new LogMessages(ERROR,
            Messages.LOG_ERROR_FAILED_TO_DETECT_PROJECT_JRE);
    public static final ILogMessage ERROR_FAILED_TO_READ_CACHED_COORDINATES = new LogMessages(ERROR,
            Messages.LOG_ERROR_FAILED_TO_READ_CACHED_COORDINATES);
    public static final ILogMessage ERROR_FAILED_TO_READ_MANUAL_MAPPINGS = new LogMessages(ERROR,
            Messages.LOG_ERROR_FAILED_TO_READ_MANUAL_MAPPINGS);
    public static final ILogMessage ERROR_FAILED_TO_REGISTER_PROJECT_DEPENDENCIES = new LogMessages(ERROR,
            Messages.LOG_ERROR_FAILED_TO_REGISTER_PROJECT_DEPENDENCIES);
    public static final ILogMessage ERROR_FAILED_TO_SEARCH_FOR_PROJECT_DEPENDENCIES = new LogMessages(ERROR,
            Messages.LOG_ERROR_FAILED_TO_SEARCH_FOR_PROJECT_DEPENDENCIES);
    public static final ILogMessage ERROR_FAILED_TO_WRITE_CACHED_COORDINATES = new LogMessages(ERROR,
            Messages.LOG_ERROR_FAILED_TO_WRITE_CACHED_COORDINATES);
    public static final ILogMessage ERROR_FAILED_TO_WRITE_MANUAL_MAPPINGS = new LogMessages(ERROR,
            Messages.LOG_ERROR_FAILED_TO_WRITE_MANUAL_MAPPINGS);
    public static final ILogMessage ERROR_IN_ADVISOR_SERVICE_SUGGEST = new LogMessages(ERROR,
            Messages.LOG_ERROR_IN_ADVISOR_SERVICE_SUGGEST);

    private LogMessages(int severity, String message)

    {
        super(severity, code++, message);
    }

    @Override
    public Bundle bundle() {
        return BUNDLE;
    }
}
