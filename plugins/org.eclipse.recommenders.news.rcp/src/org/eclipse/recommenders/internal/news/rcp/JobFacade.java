/**
 * Copyright (c) 2015 Pawel Nowak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.recommenders.internal.news.rcp;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.recommenders.news.rcp.IFeedMessage;
import org.eclipse.recommenders.news.rcp.IJobFacade;
import org.eclipse.recommenders.news.rcp.IPollFeedJob;

@SuppressWarnings("restriction")
public class JobFacade implements IJobFacade {

    private static final long START_DELAY = 0;

    @Override
    public boolean jobExists(IPollFeedJob job) {
        if (PollFeedJob.getJobManager().find(job).length > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void schedule() {
        // making sure only 1 job is running
        if (!jobExists(getJob())) {
            Job job = (Job) getJob();
            job.schedule();
        }

    }

    @Override
    public IPollFeedJob getJob() {
        // TODO return single job instance
        return null;
    }

    @Override
    public void jobDone() {
        // organize messages here
        // reschedule according to users preference, lets say by default 30 minutes

    }

    @Override
    public Map<FeedDescriptor, List<IFeedMessage>> getMessages() {
        // TODO return messages
        return null;
    }

}
