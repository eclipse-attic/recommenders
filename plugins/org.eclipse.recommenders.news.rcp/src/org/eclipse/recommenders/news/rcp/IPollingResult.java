/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.news.rcp;

import java.util.List;

public interface IPollingResult {

    public enum Status {
        OK,
        FEEDS_NOT_POLLED_YET,
        FEED_NOT_FOUND_AT_URL,
        ERROR_CONNECTING_TO_FEED
    }

    public Status getStatus();

    public List<IFeedMessage> getMessages();
}
