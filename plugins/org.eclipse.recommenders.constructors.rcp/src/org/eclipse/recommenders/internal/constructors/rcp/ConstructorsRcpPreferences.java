/**
 * Copyright (c) 2015 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andreas Sewe - initial API and implementation.
 */
package org.eclipse.recommenders.internal.constructors.rcp;

import static org.eclipse.recommenders.completion.rcp.Constants.*;

import javax.inject.Inject;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.extensions.Preference;

@SuppressWarnings("restriction")
public class ConstructorsRcpPreferences {

    /**
     * The minimum percentage (in the range [0, 100]) that a proposal needs to have before displaying it in the UI.
     */
    @Inject
    @Preference(PREF_MIN_PROPOSAL_PERCENTAGE)
    public int minProposalPercentage;

    @Inject
    @Preference(PREF_MAX_NUMBER_OF_PROPOSALS)
    public int maxNumberOfProposals;

    @Inject
    @Preference(PREF_UPDATE_PROPOSAL_RELEVANCE)
    public boolean changeProposalRelevance;

    @Inject
    @Preference(PREF_DECORATE_PROPOSAL_ICON)
    public boolean decorateProposalIcon;

    @Inject
    @Preference(PREF_DECORATE_PROPOSAL_TEXT)
    public boolean decorateProposalText;

    @Inject
    @Preference
    public IEclipsePreferences store;
}
