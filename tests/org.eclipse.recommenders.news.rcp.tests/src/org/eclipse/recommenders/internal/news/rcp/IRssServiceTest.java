/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import static org.eclipse.recommenders.internal.news.rcp.TestUtils.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import org.eclipse.mylyn.commons.notifications.core.NotificationEnvironment;
import org.eclipse.recommenders.news.rcp.IJobFacade;
import org.eclipse.recommenders.news.rcp.IPollFeedJob;
import org.eclipse.recommenders.news.rcp.INewsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("restriction")
public class IRssServiceTest {

    private static final String FIRST_ELEMENT = "first";
    private NotificationEnvironment environment;
    private NewsRcpPreferences preferences;
    private EventBus bus;
    private IPollFeedJob job;
    private IJobFacade jobFacade;

    @Before
    public void setUp() {
        environment = mock(NotificationEnvironment.class);
        preferences = mock(NewsRcpPreferences.class);
        bus = mock(EventBus.class);
        job = mock(PollFeedJob.class);
        jobFacade = mock(JobFacade.class);
    }

    @Test
    public void testStartEnabledFeed() {
        FeedDescriptor feed = enabled(FIRST_ELEMENT);
        when(preferences.isEnabled()).thenReturn(true);
        when(preferences.getFeedDescriptors()).thenReturn(ImmutableList.of(feed));
        INewsService service = new NewsService(preferences, bus, environment, jobFacade);
        service.start();
        verify(jobFacade, times(1)).schedule();
    }

    @Test
    public void testStartDisabledFeed() {
        FeedDescriptor feed = disabled(FIRST_ELEMENT);
        when(preferences.isEnabled()).thenReturn(true);
        when(preferences.getFeedDescriptors()).thenReturn(ImmutableList.of(feed));
        INewsService service = new NewsService(preferences, bus, environment, jobFacade);
        service.start();
        verify(jobFacade, times(0)).schedule();
    }

    @Test
    public void testStartDisabledPreferences() {
        FeedDescriptor feed = enabled(FIRST_ELEMENT);
        when(preferences.isEnabled()).thenReturn(false);
        when(preferences.getFeedDescriptors()).thenReturn(ImmutableList.of(feed));
        INewsService service = new NewsService(preferences, bus, environment, jobFacade);
        service.start();
        verify(jobFacade, times(0)).schedule();
    }

    @Test
    public void testGetMessages() {
        FeedDescriptor feed = enabled(FIRST_ELEMENT);
        when(preferences.isEnabled()).thenReturn(true);
        when(preferences.getFeedDescriptors()).thenReturn(ImmutableList.of(feed));
        INewsService service = new NewsService(preferences, bus, environment, jobFacade);
        service.start();
        assertThat(service.getMessages(3).size(), is(3));
    }

}
