/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.news.rcp;

import java.util.Map;

import org.eclipse.recommenders.internal.news.rcp.FeedDescriptor;
import org.eclipse.recommenders.internal.news.rcp.PollingResult;

import com.google.common.eventbus.EventBus;

public interface INotificationFacade {

    /**
     * Displays notification.
     *
     * @param messages
     *            New (that has been polled for the first time) messages
     * @param bus
     *            EventBus that will handle message events
     */
    void displayNotification(Map<FeedDescriptor, PollingResult> messages, EventBus bus);

}
