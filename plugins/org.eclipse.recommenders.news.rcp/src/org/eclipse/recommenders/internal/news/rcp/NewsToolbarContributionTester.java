/**
 * Copyright (c) 2015 Codetrails GmbH. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Johannes Dorn - initial API and implementation.
 */
package org.eclipse.recommenders.internal.news.rcp;

import static org.eclipse.recommenders.internal.news.rcp.Constants.PREF_NEWS_ENABLED;

import javax.inject.Inject;

import org.eclipse.core.expressions.PropertyTester;

public class NewsToolbarContributionTester extends PropertyTester {

    @Inject
    private NewsRcpPreferences preferences;

    // old constructor
    @Inject
    public NewsToolbarContributionTester(NewsRcpPreferences preferences) {
        this.preferences = preferences;
    }

    // this class is called by extension point so it must have default no-arg constructor
    public NewsToolbarContributionTester() {
        DIUtil.initiateContext(this);
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!NewsService.isRealEclipse()) {
            return false;
        }
        if (PREF_NEWS_ENABLED.equals(property)) {
            return preferences.isEnabled();
        }
        return false;
    }

}
