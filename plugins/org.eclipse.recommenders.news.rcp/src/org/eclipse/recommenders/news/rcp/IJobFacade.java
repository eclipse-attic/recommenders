/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.news.rcp;

import java.util.List;
import java.util.Map;

import org.eclipse.recommenders.internal.news.rcp.FeedDescriptor;

public interface IJobFacade {

    void schedule();

    IPollFeedJob getJob();

    void jobDone();

    Map<FeedDescriptor, List<IFeedMessage>> getMessages();

    boolean jobExists(IPollFeedJob job);
}
