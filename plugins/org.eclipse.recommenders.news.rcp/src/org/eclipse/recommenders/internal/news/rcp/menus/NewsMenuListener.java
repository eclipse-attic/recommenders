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
            String menuName = entry.getKey().getName();
            if (containsUnreadMessages(entry.getValue())) {
                menuName = menuName.concat(" (" + getUnreadMessagesNumber(entry.getValue()) + ")");
            }
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
    }

    private void addMarkAsReadAction(FeedDescriptor feed, MenuManager menu) {
        menu.add(new Separator());
        menu.add(newMarkFeedAsReadAction(eventBus, feed));
    }

    private void groupEntries(MenuManager menu, List<IFeedMessage> messages) {
        List<List<IFeedMessage>> groupedMessages = splitMessagesByAge(messages);
        if (!groupedMessages.get(TODAY).isEmpty()) {
            addLabel(menu, Messages.LABEL_TODAY);
            addMessages(menu, groupedMessages.get(TODAY));
        }
        if (!groupedMessages.get(YESTERDAY).isEmpty()) {
            addLabel(menu, Messages.LABEL_YESTERDAY);
            addMessages(menu, groupedMessages.get(YESTERDAY));
        }
        if (!groupedMessages.get(THIS_WEEK).isEmpty()) {
            addLabel(menu, Messages.LABEL_THIS_WEEK);
            addMessages(menu, groupedMessages.get(THIS_WEEK));
        }
        if (!groupedMessages.get(LAST_WEEK).isEmpty()) {
            addLabel(menu, Messages.LABEL_LAST_WEEK);
            addMessages(menu, groupedMessages.get(LAST_WEEK));
        }
        if (!groupedMessages.get(THIS_MONTH).isEmpty()) {
            addLabel(menu, Messages.LABEL_THIS_MONTH);
            addMessages(menu, groupedMessages.get(THIS_MONTH));
        }
        if (!groupedMessages.get(LAST_MONTH).isEmpty()) {
            addLabel(menu, Messages.LABEL_LAST_MONTH);
            addMessages(menu, groupedMessages.get(LAST_MONTH));
        }
        if (!groupedMessages.get(THIS_YEAR).isEmpty()) {
            addLabel(menu, Messages.LABEL_THIS_YEAR);
            addMessages(menu, groupedMessages.get(THIS_YEAR));
        }
        if (!groupedMessages.get(OLDER).isEmpty()) {
            addLabel(menu, Messages.LABEL_OLDER_ENTRIES);
            addMessages(menu, groupedMessages.get(OLDER));
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
            action.setText(message.getTitle());
            if (!message.isRead()) {
                action.setText(Messages.UNREAD_MESSAGE_PREFIX.concat(action.getText()));
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

    private boolean containsUnreadMessages(List<IFeedMessage> messages) {
        for (IFeedMessage message : messages) {
            if (!message.isRead()) {
                return true;
            }
        }
        return false;
    }

}
