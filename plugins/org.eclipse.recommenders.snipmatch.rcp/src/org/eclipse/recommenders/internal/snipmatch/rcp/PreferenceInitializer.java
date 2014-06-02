/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.snipmatch.rcp;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    private static final String REPO_URL = "https://git.eclipse.org/r/recommenders/org.eclipse.recommenders.snipmatch.snippets"; //$NON-NLS-1$

    @Override
    public void initializeDefaultPreferences() {
        String configuration = new EclipseGitSnippetRepositoryProvider().convert(new EclipseGitSnippetRepositoryConfiguration(
                REPO_URL, true));

        ScopedPreferenceStore store = new ScopedPreferenceStore(DefaultScope.INSTANCE, Constants.BUNDLE_ID);
        store.setDefault(Constants.PREF_SNIPPETS_REPO, configuration);
    }
}
