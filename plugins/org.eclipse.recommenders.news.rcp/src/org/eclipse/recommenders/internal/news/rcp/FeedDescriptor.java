/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import static org.eclipse.recommenders.internal.news.rcp.Constants.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.recommenders.internal.news.rcp.l10n.Messages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.google.common.base.Preconditions;

public class FeedDescriptor {

    private final boolean defaultRepository;
    private final String id;
    private final URL url;
    private final String name;
    private final String pollingInterval;
    private final String description;
    private final String iconPath;
    private boolean enabled;

    public FeedDescriptor(FeedDescriptor that) {
        this(that.getId(), that.getUrl(), that.getName(), that.isEnabled(), that.isDefaultRepository(),
                that.getPollingInterval(), that.getDescription(), that.getIconPath());
    }

    public FeedDescriptor(IConfigurationElement config, boolean enabled) {
        this(config.getAttribute(ATTRIBUTE_ID), stringToUrl(config.getAttribute(ATTRIBUTE_URL)),
                config.getAttribute(ATTRIBUTE_NAME), enabled, true, config.getAttribute(ATTRIBUTE_POLLING_INTERVAL),
                config.getAttribute(ATTRIBUTE_DESCRIPTION), config.getAttribute(ATTRIBUTE_ICON));
        Preconditions.checkNotNull(getId());
        Preconditions.checkArgument(isUrlValid(config.getAttribute(ATTRIBUTE_URL)),
                Messages.FEED_DESCRIPTOR_MALFORMED_URL);
    }

    public FeedDescriptor(String url, String name, String pollingInterval) {
        this(url, stringToUrl(url), name, true, false, pollingInterval, null, null);
        Preconditions.checkArgument(isUrlValid(url), Messages.FEED_DESCRIPTOR_MALFORMED_URL);
    }

    private FeedDescriptor(String id, URL url, String name, boolean enabled, boolean defaultRepository,
            String pollingInterval, String description, String iconPath) {
        this.id = id;
        this.url = url;
        this.name = name;
        this.enabled = enabled;
        this.defaultRepository = defaultRepository;
        this.pollingInterval = pollingInterval;
        this.description = description;
        this.iconPath = iconPath;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public URL getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public String getPollingInterval() {
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FeedDescriptor rhs = (FeedDescriptor) obj;
        if (!getId().equals(rhs.getId())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 43;
        int result = 1;
        result = prime * result + (getId() == null ? 0 : getId().hashCode());
        return result;
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

    private static URL stringToUrl(String s) {
        try {
            return new URL(s);
        } catch (MalformedURLException e) {
            // should never happen
            return null;
        }
    }

    private String getIconPath() {
        return iconPath;
    }
}
