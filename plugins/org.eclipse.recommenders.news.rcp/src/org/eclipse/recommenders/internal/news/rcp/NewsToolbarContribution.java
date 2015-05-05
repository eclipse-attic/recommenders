/**
 * Copyright (c) 2015 Codetrails GmbH. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Johannes Dorn - initial API and implementation.
 */
package org.eclipse.recommenders.internal.news.rcp;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.recommenders.news.rcp.IFeedMessage;
import org.eclipse.recommenders.news.rcp.IRssService;
import org.eclipse.recommenders.news.rcp.IRssService.NewFeedItemsEvent;
import org.eclipse.recommenders.rcp.SharedImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class NewsToolbarContribution extends WorkbenchWindowControlContribution {

    private final IRssService service;
    private final SharedImages images;
    private final EventBus bus;

    private Button button;

    @Inject
    public NewsToolbarContribution(IRssService service, SharedImages images, EventBus bus) {
        this.service = service;
        this.images = images;
        this.bus = bus;
        bus.register(this);
    }

    @Override
    protected Control createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = GridLayoutFactory.fillDefaults().create();
        composite.setLayout(layout);

        button = new Button(composite, SWT.NONE);
        button.setImage(images.getImage(SharedImages.Images.OBJ_NEWSLETTER));

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                Map<FeedDescriptor, List<IFeedMessage>> messages = service.getMessages(3);
                // TODO create notification with clickable links
                for (Entry<FeedDescriptor, List<IFeedMessage>> entry : messages.entrySet()) {
                    System.out.println(entry.getKey().getName());
                    for (IFeedMessage message : entry.getValue()) {
                        System.out.println("-- " + message.getTitle() + " - " + message.getDate());
                        System.out.println("  -- " + message.getUrl());
                    }
                }
                PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        button.setImage(images.getImage(SharedImages.Images.OBJ_CONTAINER));
                    }
                });
            }
        });
        return composite;
    }

    @Subscribe
    public void handle(NewFeedItemsEvent event) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
                button.setImage(images.getImage(SharedImages.Images.OBJ_NEWSLETTER));
            }
        });
    }
}
