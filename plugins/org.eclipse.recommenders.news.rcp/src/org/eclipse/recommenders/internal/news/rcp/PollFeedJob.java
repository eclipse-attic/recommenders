/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.commons.notifications.core.NotificationEnvironment;
import org.eclipse.mylyn.internal.commons.notifications.feed.FeedEntry;
import org.eclipse.mylyn.internal.commons.notifications.feed.FeedReader;
import org.eclipse.recommenders.news.rcp.IFeedMessage;
import org.eclipse.recommenders.news.rcp.IPollFeedJob;
import org.eclipse.recommenders.utils.Urls;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@SuppressWarnings("restriction")
public class PollFeedJob extends Job implements IPollFeedJob {
    private final String pluginId;
    private final NewsRcpPreferences preferences;
    private final NotificationEnvironment environment;
    private List<? extends IFeedMessage> messages = Lists.newArrayList();
    private Set<FeedDescriptor> feeds = Sets.newHashSet();

    public PollFeedJob(NewsRcpPreferences preferences, NotificationEnvironment environment) {
        super(Constants.PLUGIN_ID);
        pluginId = Constants.PLUGIN_ID;
        Preconditions.checkNotNull(preferences);
        Preconditions.checkNotNull(environment);
        this.preferences = preferences;
        this.environment = environment;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        Map<FeedDescriptor, Date> map = Maps.newHashMap();
        for (FeedDescriptor feed : feeds) {
            // poll feed
            map.put(feed, new Date());
            // its just mock, put it here so you can know where it's called
            try {
                readMessages(null, monitor, null);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return Status.OK_STATUS;
    }

    @Override
    public boolean belongsTo(Object job) {
        if (job == null) {
            return false;
        }
        if (!(job instanceof PollFeedJob)) {
            return false;
        }
        PollFeedJob rhs = (PollFeedJob) job;
        if (!pluginId.equals(rhs.getPluginId())) {
            return false;
        }
        return true;
    }

    private boolean isFeedEnabled(FeedDescriptor feed) {
        for (FeedDescriptor fd : preferences.getFeedDescriptors()) {
            if (feed.getId().equals(fd.getId())) {
                return fd.isEnabled();
            }
        }
        return false;
    }

    private List<? extends IFeedMessage> readMessages(InputStream in, IProgressMonitor monitor, String eventId)
            throws IOException {
        FeedReader reader = new FeedReader(eventId, environment);
        reader.parse(in, monitor);
        return FluentIterable.from(reader.getEntries()).transform(new Function<FeedEntry, IFeedMessage>() {

            @Override
            public IFeedMessage apply(FeedEntry entry) {
                return new FeedMessage(entry.getId(), entry.getDate(), entry.getDescription(), entry.getTitle(),
                        Urls.toUrl(entry.getUrl()));
            }
        }).toList();
    }

    @Override
    public List<? extends IFeedMessage> getMessages() {
        return messages;
    }

    @Override
    public void setFeeds(Collection<FeedDescriptor> feeds) {
        this.feeds = (Set<FeedDescriptor>) feeds;

    }

    public String getPluginId() {
        return pluginId;
    }
}
