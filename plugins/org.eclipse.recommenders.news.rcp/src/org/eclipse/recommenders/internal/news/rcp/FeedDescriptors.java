/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import static com.google.common.base.Strings.nullToEmpty;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.recommenders.internal.news.rcp.l10n.LogMessages;
import org.eclipse.recommenders.utils.Logs;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FeedDescriptors {

    public static final char DISABLED_FLAG = '!';
    public static final char SEPARATOR = ';';

    private static final String EXT_ID_PROVIDER = "org.eclipse.recommenders.news.rcp.feed"; //$NON-NLS-1$

    public static List<FeedDescriptor> getRegisteredFeeds() {
        final IConfigurationElement[] elements = Platform.getExtensionRegistry()
                .getConfigurationElementsFor(EXT_ID_PROVIDER);
        Arrays.sort(elements, new Comparator<IConfigurationElement>() {

            @Override
            public int compare(IConfigurationElement lhs, IConfigurationElement rhs) {
                return lhs.getAttribute(Constants.ATTRIBUTE_NAME).compareTo(rhs.getAttribute(Constants.ATTRIBUTE_NAME));
            }
        });
        final List<FeedDescriptor> descriptors = Lists.newLinkedList();
        for (final IConfigurationElement element : elements) {
            boolean enabled = true;
            descriptors.add(new FeedDescriptor(element, enabled));
        }
        return descriptors;
    }

    @SuppressWarnings("serial")
    public static List<FeedDescriptor> getFeeds(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        Gson gson = new GsonBuilder().create();
        Type collectionType = new TypeToken<List<FeedDescriptor>>() {
        }.getType();
        List<FeedDescriptor> list = gson.fromJson(json, collectionType);
        Collections.sort(list, new Comparator<FeedDescriptor>() {

            @Override
            public int compare(FeedDescriptor lhs, FeedDescriptor rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        return list;
    }

    public static List<FeedDescriptor> load(String preferenceString, List<FeedDescriptor> available) {
        List<FeedDescriptor> result = Lists.newArrayList();
        for (String id : StringUtils.split(nullToEmpty(preferenceString), SEPARATOR)) {
            final boolean enabled;
            if (id.charAt(0) == DISABLED_FLAG) {
                enabled = false;
                id = id.substring(1);
            } else {
                enabled = true;
            }

            FeedDescriptor found = find(available, id);
            if (found != null) {
                FeedDescriptor descriptor = new FeedDescriptor(found);
                descriptor.setEnabled(enabled);
                if (!result.contains(descriptor)) {
                    result.add(descriptor);
                } else {
                    Logs.log(LogMessages.WARNING_DUPLICATE_FEED, descriptor.getId());
                }
            }
        }

        for (FeedDescriptor descriptor : available) {
            if (find(result, descriptor.getId()) == null) {
                result.add(descriptor);
            }
        }

        return result;
    }

    public static String feedsToString(List<FeedDescriptor> descriptors) {
        StringBuilder sb = new StringBuilder();
        Iterator<FeedDescriptor> it = descriptors.iterator();
        while (it.hasNext()) {
            FeedDescriptor descriptor = it.next();
            if (!sb.toString().contains(descriptor.getId())) {
                if (!descriptor.isEnabled()) {
                    sb.append(DISABLED_FLAG);
                }
                sb.append(descriptor.getId());
                if (it.hasNext()) {
                    sb.append(SEPARATOR);
                }
            }
        }
        String result = sb.toString();
        if (result.length() > 1 && result.lastIndexOf(SEPARATOR) == result.length() - 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public static String customFeedsToString(List<FeedDescriptor> feeds) {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(feeds);
    }

    private static FeedDescriptor find(List<FeedDescriptor> descriptors, String id) {
        for (FeedDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }
}
