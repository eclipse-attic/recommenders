/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.completion.rcp.processable;

import static com.google.common.base.Optional.fromNullable;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.util.Set;

import org.eclipse.swt.graphics.Image;

import com.google.common.base.Optional;

public class SessionProcessorDescriptor implements Comparable<SessionProcessorDescriptor> {

    private String id;
    private String name;
    private String description;
    private Image icon;
    private int priority;
    private boolean enabled;
    private SessionProcessor processor;
    private String preferencePageId;

    public SessionProcessorDescriptor(String id, String name, String description, Image icon, int priority,
            boolean enabled, String preferencePageId, SessionProcessor processor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.priority = priority;
        this.enabled = enabled;
        this.preferencePageId = preferencePageId;
        this.processor = processor;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return defaultString(description, ""); //$NON-NLS-1$
    }

    public Image getIcon() {
        return icon;
    }

    public int getPriority() {
        return priority;
    }

    public SessionProcessor getProcessor() {
        return processor;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enable) {
        enabled = enable;
        Set<String> disabledProcessors = SessionProcessorDescriptors.getDisabledProcessors();
        if (enable) {
            disabledProcessors.remove(id);
        } else {
            disabledProcessors.add(id);
        }
        SessionProcessorDescriptors.saveDisabledProcessors(disabledProcessors);
    }

    public Optional<String> getPreferencePage() {
        return fromNullable(preferencePageId);
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int compareTo(SessionProcessorDescriptor o) {
        String other = o.priority + o.id;
        String self = priority + id;
        return self.compareTo(other);
    }
}
