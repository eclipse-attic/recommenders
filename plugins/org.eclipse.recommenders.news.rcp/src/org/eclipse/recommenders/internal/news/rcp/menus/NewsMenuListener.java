/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp.menus;

import static org.eclipse.recommenders.internal.news.rcp.FeedEvents.createFeedMessageReadEvent;
import static org.eclipse.recommenders.internal.news.rcp.MessageUtils.*;
import static org.eclipse.recommenders.internal.news.rcp.menus.MarkAsReadAction.*;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.recommenders.internal.news.rcp.FeedDescriptor;
import org.eclipse.recommenders.internal.news.rcp.l10n.Messages;
import org.eclipse.recommenders.news.rcp.IFeedMessage;
import org.eclipse.recommenders.rcp.utils.BrowserUtils;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;

public class NewsMenuListener implements IMenuListener {
    private final EventBus eventBus;
    private Map<FeedDescriptor, List<IFeedMessage>> messages;

    public NewsMenuListener(EventBus eventBus) {
        super();
        this.eventBus = eventBus;
    }

    public void setMessages(Map<FeedDescriptor, List<IFeedMessage>> messages) {
        this.messages = messages;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager) {
        for (Entry<FeedDescriptor, List<IFeedMessage>> entry : messages.entrySet()) {
            String menuName = getMenuEntryTitle(entry.getKey().getName(), entry.getValue());
            MenuManager menu = new MenuManager(menuName, entry.getKey().getId());
            if (entry.getKey().getIcon() != null) {
                // in Kepler: The method setImageDescriptor(ImageDescriptor) is undefined for the type MenuManager
                // menu.setImageDescriptor(ImageDescriptor.createFromImage(entry.getKey().getIcon()));
            }
            groupEntries(menu, entry.getValue());
            addMarkAsReadAction(entry.getKey(), menu);
            manager.add(menu);
        }
        manager.add(new Separator());
        manager.add(newMarkAllAsReadAction(eventBus));
        manager.add(new Separator());
        manager.add(newPreferencesAction());
    }

    private void addMarkAsReadAction(FeedDescriptor feed, MenuManager menu) {
        menu.add(new Separator());
        menu.add(newMarkFeedAsReadAction(eventBus, feed));
    }

    private Action newPreferencesAction() {
        return new Action() {
            @Override
            public void run() {
                PreferencesUtil
                        .createPreferenceDialogOn(null, "org.eclipse.recommenders.news.rcp.preferencePage", null, null) //$NON-NLS-1$
                        .open();
            }

            @Override
            public String getText() {
                return Messages.LABEL_PREFERENCES;
            }
        };
    }

    private void groupEntries(MenuManager menu, List<IFeedMessage> messages) {
        List<List<IFeedMessage>> groupedMessages = splitMessagesByAge(messages);
        List<String> labels = ImmutableList.of(Messages.LABEL_TODAY, Messages.LABEL_YESTERDAY, Messages.LABEL_THIS_WEEK,
                Messages.LABEL_LAST_WEEK, Messages.LABEL_THIS_MONTH, Messages.LABEL_LAST_MONTH,
                Messages.LABEL_THIS_YEAR, Messages.LABEL_OLDER_ENTRIES);
        for (int i = 0; i <= OLDER; i++) {
            if (!groupedMessages.get(i).isEmpty()) {
                addLabel(menu, labels.get(i));
                addMessages(menu, groupedMessages.get(i));
            }
        }
    }

    private void addMessages(MenuManager menu, List<IFeedMessage> messages) {
        for (final IFeedMessage message : messages) {
            Action action = new Action() {

                @Override
                public void run() {
                    BrowserUtils.openInExternalBrowser(message.getUrl());
                    eventBus.post(createFeedMessageReadEvent(message.getId()));
                }
            };
            if (!message.isRead()) {
                action.setText(Messages.UNREAD_MESSAGE_PREFIX.concat(message.getTitle()));
            } else {
                action.setText(Messages.READ_MESSAGE_PREFIX.concat(message.getTitle()));
            }
            menu.add(action);
        }
    }

    private void addLabel(MenuManager menu, String text) {
        Action action = new Action() {
        };
        action.setText(text);
        action.setEnabled(false);
        menu.add(new Separator());
        menu.add(action);
    }

    private static String getMenuEntryTitle(String feedName, List<IFeedMessage> messages) {
        boolean read = false;
        for (IFeedMessage message : messages) {
            if (message.isRead()) {
                read = true;
            }
        }
        if (read) {
            if (getUnreadMessagesNumber(messages) > 0) {
                return Messages.UNREAD_MESSAGE_PREFIX.concat(feedName)
                        .concat(" (" + getUnreadMessagesNumber(messages) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                return Messages.READ_MESSAGE_PREFIX.concat(feedName);
            }
        } else {
            return Messages.UNREAD_MESSAGE_PREFIX.concat(feedName)
                    .concat(" (" + getUnreadMessagesNumber(messages) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

}
