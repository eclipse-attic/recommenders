/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Olav Lenz - Move to new file.
 */
package org.eclipse.recommenders.internal.models.rcp;

import static java.lang.String.format;

import java.util.Set;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.recommenders.models.ModelCoordinate;
import org.eclipse.recommenders.rcp.utils.Jobs;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

class TriggerModelDownloadActionForModelCoordinates extends Action {

    private EclipseModelRepository repo;

    private Set<ModelCoordinate> mcs = Sets.newHashSet();

    private EventBus bus;

    public TriggerModelDownloadActionForModelCoordinates(String text, EclipseModelRepository repo, EventBus bus) {
        super(text);
        this.repo = repo;
        this.bus = bus;
    }

    public TriggerModelDownloadActionForModelCoordinates(String text, Set<ModelCoordinate> mcs,
            EclipseModelRepository repo, EventBus bus) {
        this(text, repo, bus);
        this.mcs = mcs;
    }

    @Override
    public void run() {
        triggerDownloadForModelCoordinates(mcs);
    }

    public void triggerDownloadForModelCoordinates(Set<ModelCoordinate> mcs) {
        Set<Job> jobs = Sets.newHashSet();
        for (ModelCoordinate mc : mcs) {
            jobs.add(new DownloadModelArchiveJob(repo, mc, false, bus));
        }
        Jobs.sequential(format("Downloading %d model archives", jobs.size()), jobs);
    }
}
