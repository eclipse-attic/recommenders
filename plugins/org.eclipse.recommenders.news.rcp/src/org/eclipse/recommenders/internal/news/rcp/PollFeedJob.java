/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.mylyn.commons.notifications.core.NotificationEnvironment;
import org.eclipse.mylyn.internal.commons.notifications.feed.FeedEntry;
import org.eclipse.mylyn.internal.commons.notifications.feed.FeedReader;
import org.eclipse.recommenders.internal.news.rcp.l10n.LogMessages;
import org.eclipse.recommenders.news.rcp.IFeedMessage;
import org.eclipse.recommenders.news.rcp.IPollFeedJob;
import org.eclipse.recommenders.utils.Logs;
import org.eclipse.recommenders.utils.Urls;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@SuppressWarnings("restriction")
public class PollFeedJob extends Job implements IPollFeedJob {
    private final String jobId;
    private final NotificationEnvironment environment;
    private final Map<FeedDescriptor, List<IFeedMessage>> groupedMessages = Maps.newHashMap();
    private final Set<FeedDescriptor> feeds = Sets.newHashSet();
    private final Map<FeedDescriptor, Date> pollDates = Maps.newHashMap();

    public PollFeedJob(String jobId, Collection<FeedDescriptor> feeds) {
        super(jobId);
        Preconditions.checkNotNull(jobId);
        Preconditions.checkNotNull(feeds);
        this.jobId = jobId;
        this.environment = new NotificationEnvironment();
        this.feeds.addAll(feeds);
        setSystem(true);
        setPriority(DECORATE);
        setRule(new MutexRule());
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        URL url = null;
        try {
            for (FeedDescriptor feed : feeds) {
                HttpURLConnection connection = (HttpURLConnection) feed.getUrl().openConnection();
                url = connection.getURL();
                connection.connect();
                updateGroupedMessages(connection, monitor, feed);
                connection.disconnect();
                pollDates.put(feed, new Date());
            }
        } catch (IOException e) {
            Logs.log(LogMessages.ERROR_CONNECTING_URL, url, e);
            return Status.CANCEL_STATUS;
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
        if (!jobId.equals(rhs.getJobId())) {
            return false;
        }
        return true;
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
    public Map<FeedDescriptor, List<IFeedMessage>> getMessages() {
        return groupedMessages;
    }

    @Override
    public Map<FeedDescriptor, Date> getPollDates() {
        return pollDates;
    }

    public String getJobId() {
        return jobId;
    }

    private void updateGroupedMessages(HttpURLConnection connection, IProgressMonitor monitor, FeedDescriptor feed) {
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK && monitor.isCanceled()) {
                return;
            }
            try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                List<IFeedMessage> messages = Lists.newArrayList(readMessages(in, monitor, feed.getId()));
                groupedMessages.put(feed, messages);
            }
        } catch (IOException e) {
            Logs.log(LogMessages.ERROR_FETCHING_MESSAGES, feed.getUrl(), e);
        }
    }

    class MutexRule implements ISchedulingRule {

        @Override
        public boolean contains(ISchedulingRule rule) {
            return rule == this;
        }

        @Override
        public boolean isConflicting(ISchedulingRule rule) {
            return rule == this;
        }

    }
}
