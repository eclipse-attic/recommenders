/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.mylyn.commons.notifications.core.NotificationEnvironment;
import org.eclipse.recommenders.internal.news.rcp.FeedEvents.FeedJobDoneEvent;
import org.eclipse.recommenders.internal.news.rcp.FeedEvents.FeedMessageReadEvent;
import org.eclipse.recommenders.news.rcp.IFeedMessage;
import org.eclipse.recommenders.news.rcp.IJobFacade;
import org.eclipse.recommenders.news.rcp.INewsService;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("restriction")
public class NewsService implements INewsService {

    private final NewsRcpPreferences preferences;
    private final NewsFeedProperties newsFeedProperties;
    private final Set<String> readIds;
    private final IJobFacade jobFacade;
    private final Map<FeedDescriptor, Date> pollDates;

    private HashMap<FeedDescriptor, List<IFeedMessage>> groupedMessages = Maps.newHashMap();

    public NewsService(NewsRcpPreferences preferences, EventBus bus, NotificationEnvironment environment,
            IJobFacade jobFacade) {
        this.preferences = preferences;
        bus.register(this);
        newsFeedProperties = new NewsFeedProperties();
        readIds = newsFeedProperties.getReadIds();
        pollDates = newsFeedProperties.getPollDates();
        this.jobFacade = jobFacade;
    }

    @Override
    public void start() {
        Set<FeedDescriptor> feeds = Sets.newHashSet();
        if (!preferences.isEnabled()) {
            return;
        }
        for (final FeedDescriptor feed : preferences.getFeedDescriptors()) {
            if (feed.isEnabled()) {
                feeds.add(feed);
            }
        }
        jobFacade.schedule(feeds);
    }

    @Override
    public Map<FeedDescriptor, List<IFeedMessage>> getMessages(final int countPerFeed) {
        jobFacade.getMessages();
        // limit those messages to countPerFeed and return
        return null;
    }

    @Subscribe
    @Override
    public void handleMessageRead(FeedMessageReadEvent event) {
        readIds.add(event.getId());
        newsFeedProperties.writeReadIds(readIds);
    }

    @Subscribe
    @Override
    public void handleJobDoneEvent(FeedJobDoneEvent event) {
        pollDates.put(event.getFeed(), event.getDate());
        newsFeedProperties.writePollDates(pollDates);
        this.groupedMessages = (HashMap<FeedDescriptor, List<IFeedMessage>>) jobFacade.getMessages();
    }

    @Override
    public boolean shouldPoll(FeedDescriptor feed) {
        newsFeedProperties.getPollDates();
        // check condition
        return false;
    }

    @Override
    public void removeFeed(FeedDescriptor feed) {
        if (groupedMessages.containsKey(feed)) {
            groupedMessages.remove(feed);
        }
    }
}
