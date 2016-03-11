/**
 * Copyright (c) 2016 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andreas Sewe - initial API and implementation.
 */
package org.eclipse.recommenders.internal.news.rcp;

public final class CommandConstants {

    private CommandConstants() {
        throw new AssertionError();
    }

    public static final String ID_OPEN_BROWSER = "org.eclipse.ui.browser.openBrowser";

    public static final String PARAMETER_OPEN_BROWSER_URL = "url";

    public static final String ID_READ_NEWS_ITEMS = "org.eclipse.recommenders.news.rcp.command.readNewsItems";

    public static final String PARAMETER_READ_NEWS_ITEMS_NEWS_ITEMS = "org.eclipse.recommenders.news.rcp.commandParameter.newsItems";

    public static final String PARAMETER_READ_NEWS_ITEMS_OPEN_BROWSER = "org.eclipse.recommenders.news.rcp.commandParameter.openBrowser";

    public static final String ID_POLL_NEWS_FEEDS = "org.eclipse.recommenders.news.rcp.command.pollNewsFeeds";

    public static final String ID_PREFERENCES = "org.eclipse.ui.window.preferences";

    public static final String PARAMETER_PREFERENCES_PREFERENCE_PAGE_ID = "preferencePageId";
}
