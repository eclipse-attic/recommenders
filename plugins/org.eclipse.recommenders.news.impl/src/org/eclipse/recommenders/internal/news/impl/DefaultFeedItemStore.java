/**
 * Copyright (c) 2016 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Johannes Dorn - initial API and implementation.
 */
package org.eclipse.recommenders.internal.news.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.mylyn.commons.notifications.core.NotificationEnvironment;
import org.eclipse.mylyn.internal.commons.notifications.feed.FeedEntry;
import org.eclipse.mylyn.internal.commons.notifications.feed.FeedReader;
import org.eclipse.recommenders.news.api.FeedItem;
import org.eclipse.recommenders.news.api.IFeedItemStore;

@SuppressWarnings("restriction")
public class DefaultFeedItemStore implements IFeedItemStore {

    private final Map<URI, List<FeedItem>> cache = new ConcurrentHashMap<>();

    private final NotificationEnvironment environment = new NotificationEnvironment();

    @Override
    public List<FeedItem> udpate(URI feedUri, InputStream stream, @Nullable IProgressMonitor monitor)
            throws IOException {
        SubMonitor progress = SubMonitor.convert(monitor, 1);
        try {
            List<FeedItem> updatedItems = parseFeedItems(stream, feedUri.toString(), progress.newChild(1));

            List<FeedItem> oldItems;
            if (updatedItems.isEmpty()) {
                oldItems = cache.remove(feedUri);
            } else {
                oldItems = cache.put(feedUri, updatedItems);
            }

            Set<String> oldIds = getIds(oldItems);
            List<FeedItem> newItems = new ArrayList<>();

            for (FeedItem updatedItem : updatedItems) {
                if (!oldIds.contains(updatedItem.getId())) {
                    newItems.add(updatedItem);
                }
            }

            return newItems;
        } finally {
            stream.close();

            if (monitor != null) {
                monitor.done();
            }
        }
    }

    @Override
    public List<FeedItem> getFeedItems(URI feedUri) {
        List<FeedItem> cachedItems = cache.get(feedUri);
        return cachedItems != null ? cachedItems : Collections.<FeedItem>emptyList();
    }

    private List<FeedItem> parseFeedItems(InputStream in, String eventId, SubMonitor monitor) throws IOException {
        SubMonitor progress = SubMonitor.convert(monitor, 100);

        FeedReader reader = new FeedReader(eventId, environment);
        IStatus status = reader.parse(in, progress.newChild(80));

        if (status.isOK()) {
            return convertEntries(reader.getEntries(), progress.newChild(20));
        } else {
            progress.setWorkRemaining(0);

            Throwable exception = status.getException();
            if (exception instanceof Exception) {
                throw new IOException((Exception) exception);
            }

            return Collections.emptyList();
        }
    }

    private List<FeedItem> convertEntries(List<FeedEntry> entries, SubMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, entries.size());

        List<FeedItem> items = new ArrayList<>(entries.size());

        for (FeedEntry entry : entries) {
            try {
                URI uri = new URI(entry.getUrl());
                FeedItem item = new FeedItem(entry.getTitle(), entry.getDate(), uri, entry.getId());
                items.add(item);
            } catch (URISyntaxException e) {
                continue;
            } finally {
                progress.worked(1);
            }
        }

        return items;
    }

    private Set<String> getIds(@Nullable List<FeedItem> items) {
        if (items == null) {
            return Collections.emptySet();
        }

        Set<String> ids = new HashSet<>();

        for (FeedItem item : items) {
            ids.add(item.getId());
        }

        return ids;
    }
}
