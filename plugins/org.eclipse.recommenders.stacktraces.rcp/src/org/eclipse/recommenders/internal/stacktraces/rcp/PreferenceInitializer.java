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
package org.eclipse.recommenders.internal.stacktraces.rcp;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {

        IEclipsePreferences s = DefaultScope.INSTANCE.getNode("org.eclipse.recommenders.stacktraces.rcp");
        s.put("server", "http://recommenders.eclipse.org/stats/stacktraces/0.1.0/new/");
        s.put("name", "anynmous");
        s.put("email", "no-reply@eclipse.org");
        s.put("mode", "ask");
    }

}
