/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import static java.lang.Boolean.TRUE;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class FeedDescriptors {

    private static final String ENABLED_BY_DEFAULT_ATTRIBUTE = "enabledByDefault"; //$NON-NLS-1$

    public static final char DISABLED_FLAG = '!';
    public static final char SEPARATOR = ';';

    private static final String EXT_ID_PROVIDER = "org.eclipse.recommenders.news.rcp.feed"; //$NON-NLS-1$

    public static List<FeedDescriptor> getRegisteredFeeds() {
        final IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
                EXT_ID_PROVIDER);
        Arrays.sort(elements, new Comparator<IConfigurationElement>() {

            @Override
            public int compare(IConfigurationElement lhs, IConfigurationElement rhs) {
                return lhs.getAttribute("name").compareTo(rhs.getAttribute("name"));
            }
        });
        final List<FeedDescriptor> descriptors = Lists.newLinkedList();
        for (final IConfigurationElement element : elements) {
            boolean enabled = Boolean.valueOf(Objects.firstNonNull(element.getAttribute(ENABLED_BY_DEFAULT_ATTRIBUTE),
                    TRUE.toString()));
            descriptors.add(new FeedDescriptor(element, enabled));
        }
        return descriptors;
    }

    public static List<FeedDescriptor> load(String preferenceString, List<FeedDescriptor> available) {
        if (Strings.isNullOrEmpty(preferenceString)) {
            return Collections.emptyList();
        }
        List<FeedDescriptor> result = Lists.newArrayList();
        for (String id : StringUtils.split(preferenceString, SEPARATOR)) {
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
                result.add(descriptor);
            }
        }

        for (FeedDescriptor descriptor : available) {
            if (find(result, descriptor.getId()) == null) {
                result.add(descriptor);
            }
        }

        return result;
    }

    public static String store(List<FeedDescriptor> descriptors) {
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

    private static FeedDescriptor find(List<FeedDescriptor> descriptors, String id) {
        for (FeedDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }
}
