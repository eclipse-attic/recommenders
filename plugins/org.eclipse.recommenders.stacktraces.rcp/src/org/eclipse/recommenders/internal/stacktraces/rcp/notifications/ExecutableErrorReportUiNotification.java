/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Daniel Haftstein - initial API and implementation.
 */
package org.eclipse.recommenders.internal.stacktraces.rcp.notifications;

import java.util.List;

public abstract class ExecutableErrorReportUiNotification extends ErrorReportUiNotification {
    public ExecutableErrorReportUiNotification(String eventId) {
        super(eventId);
    }

    public static abstract class Action {
        private final String name;

        public Action(String name) {
            this.name = name;
        }

        public abstract void execute();

        public String getName() {
            return name;
        }
    }

    @Override
    public final void open() {
        if (!getActions().isEmpty()) {
            getActions().get(0).execute();
        }
    }

    /**
     * Returns the list of actions for this notification. The first action (if present) will be selected as default
     * action for the notification and used in {@link #open()}
     */
    public abstract List<Action> getActions();

}
