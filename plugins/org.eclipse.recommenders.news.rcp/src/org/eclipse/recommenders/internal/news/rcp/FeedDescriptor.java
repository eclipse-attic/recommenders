/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.eclipse.recommenders.internal.news.rcp.Constants.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.recommenders.internal.news.rcp.l10n.LogMessages;
import org.eclipse.recommenders.internal.news.rcp.l10n.Messages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.google.common.base.Preconditions;

public class FeedDescriptor implements Comparable<FeedDescriptor> {

    private final boolean defaultRepository;
    private final String id;
    private final URI uri;
    private final String name;
    private final long pollingInterval;
    private final String description;
    private final String iconPath;
    private final String contributedBy;
    private boolean enabled;

    public FeedDescriptor(FeedDescriptor that) {
        this(that.getId(), that.getUri().toString(), that.getName(), that.isEnabled(), that.isDefaultRepository(),
                that.getPollingInterval(), that.getDescription(), that.getIconPath(), that.getContributedBy());
    }

    public FeedDescriptor(String uri, String name, long pollingInterval) {
        this(uri, uri, name, true, false, pollingInterval, null, null, null);
    }

    private FeedDescriptor(String id, String uri, String name, boolean enabled, boolean defaultRepository,
            long pollingInterval, String description, String iconPath, String contributedBy) {
        Objects.requireNonNull(id);
        Preconditions.checkArgument(isUrlValid(uri), Messages.FEED_DESCRIPTOR_MALFORMED_URL);

        this.id = id;
        this.uri = stringToUrl(uri);
        this.name = name;
        this.enabled = enabled;
        this.defaultRepository = defaultRepository;
        this.pollingInterval = pollingInterval;
        this.description = description;
        this.iconPath = iconPath;
        this.contributedBy = contributedBy;
    }

    public String getContributedBy() {
        return contributedBy;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public URI getUri() {
        return uri;
    }

    public String getDescription() {
        return description;
    }

    public long getPollingInterval() {
        return pollingInterval;
    }

    public boolean isDefaultRepository() {
        return defaultRepository;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Image getIcon() {
        if (iconPath != null) {
            return AbstractUIPlugin.imageDescriptorFromPlugin(Constants.PLUGIN_ID, iconPath).createImage();
        }
        return null;
    }

    private String getIconPath() {
        return iconPath;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        FeedDescriptor that = (FeedDescriptor) other;
        return Objects.equals(this.getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    public static boolean isUrlValid(String url) {
        URL u;
        try {
            u = new URL(url);
            u.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
        return true;
    }

    private static URI stringToUrl(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            Logs.log(LogMessages.ERROR_FEED_MALFORMED_URL, url);
            return null;
        }
    }

    @Override
    public int compareTo(FeedDescriptor that) {
        return this.getName().compareTo(that.getName());
    }

    public static FeedDescriptor fromConfigurationElement(IConfigurationElement config, boolean enabled, String contributedBy) {
        String id = config.getAttribute(ATTRIBUTE_ID);
        String uri = config.getAttribute(ATTRIBUTE_URI);
        String name = config.getAttribute(ATTRIBUTE_NAME);
        String pollingIntervalAttribute = config.getAttribute(ATTRIBUTE_POLLING_INTERVAL);
        long pollingInterval;
        if (pollingIntervalAttribute != null) {
            pollingInterval = Long.parseLong(pollingIntervalAttribute);
        } else {
            pollingInterval = HOURS.toMinutes(8);
        }
        String description = config.getAttribute(ATTRIBUTE_DESCRIPTION);
        String icon = config.getAttribute(ATTRIBUTE_ICON);

        return new FeedDescriptor(id, uri, name,
                enabled, true, pollingInterval,
                description, icon, contributedBy);
    }
}
