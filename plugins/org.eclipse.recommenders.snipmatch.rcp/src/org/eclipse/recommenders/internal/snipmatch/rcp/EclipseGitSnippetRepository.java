/**
 * Copyright (c) 2010, 2014 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.snipmatch.rcp;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.recommenders.injection.InjectionService;
import org.eclipse.recommenders.rcp.model.EclipseGitSnippetRepositoryConfiguration;
import org.eclipse.recommenders.rcp.model.SnipmatchRcpModelFactory;
import org.eclipse.recommenders.snipmatch.GitSnippetRepository;
import org.eclipse.recommenders.snipmatch.GitSnippetRepository.GitUpdateException;
import org.eclipse.recommenders.snipmatch.ISnippet;
import org.eclipse.recommenders.snipmatch.ISnippetRepository;
import org.eclipse.recommenders.snipmatch.model.SnippetRepositoryConfiguration;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.Urls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.name.Names;

public class EclipseGitSnippetRepository implements ISnippetRepository {

    private static Logger LOG = LoggerFactory.getLogger(EclipseGitSnippetRepository.class);

    private final EventBus bus;

    private volatile int timesOpened;
    private ISnippetRepository delegate;
    private volatile boolean delegateOpen;

    private final Lock readLock;
    private final Lock writeLock;

    private volatile Job openJob = null;

    public EclipseGitSnippetRepository(int id, File basedir, String remoteUri, String pushUrl, String pushBranchPrefix,
            EventBus bus) {
        this.bus = bus;

        delegate = new GitSnippetRepository(id, new File(basedir, Urls.mangle(remoteUri)), remoteUri, pushUrl,
                pushBranchPrefix);

        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
    }

    @Override
    public void open() {
        writeLock.lock();
        try {
            timesOpened++;
            if (timesOpened > 1) {
                return;
            }
            if (openJob == null && !delegateOpen) {
                openJob = new Job(Messages.JOB_OPENING_SNIPPET_REPOSITORY) {

                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            delegate.open();
                            changeStateToOpen();
                            return Status.OK_STATUS;
                        } catch (GitUpdateException e) {
                            changeStateToOpen();
                            Status status = new Status(IStatus.WARNING, Constants.BUNDLE_ID, MessageFormat.format(
                                    Messages.WARNING_FAILURE_TO_UPDATE_REPOSITORY, delegate.getRepositoryLocation(),
                                    e.getMessage()), e);
                            Platform.getLog(Platform.getBundle(Constants.BUNDLE_ID)).log(status);
                            return Status.OK_STATUS;
                        } catch (IOException e) {
                            LOG.error("Exception while opening repository.", e); //$NON-NLS-1$
                            Status status = new Status(IStatus.ERROR, Constants.BUNDLE_ID, MessageFormat.format(
                                    Messages.ERROR_FAILURE_TO_CLONE_REPOSITORY, delegate.getRepositoryLocation(),
                                    e.getMessage()), e);
                            Platform.getLog(Platform.getBundle(Constants.BUNDLE_ID)).log(status);
                            return Status.CANCEL_STATUS;
                        } finally {
                            openJob = null;
                        }
                    }

                    private void changeStateToOpen() {
                        delegateOpen = true;
                        bus.post(new SnippetRepositoryOpenedEvent(EclipseGitSnippetRepository.this));
                    }
                };
                openJob.schedule();
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        writeLock.lock();
        try {
            if (timesOpened == 0) {
                return;
            } else if (timesOpened > 1) {
                timesOpened--;
                return;
            } else if (timesOpened == 1) {
                timesOpened = 0;
                if (openJob != null) {
                    try {
                        openJob.join();
                        openJob = null;
                    } catch (InterruptedException e) {
                        LOG.error("Failed to join open job", e); //$NON-NLS-1$
                    }
                }
                delegate.close();
                delegateOpen = false;
                bus.post(new SnippetRepositoryClosedEvent(this));
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<Recommendation<ISnippet>> search(String query) {
        readLock.lock();
        try {
            if (!isOpen() || !delegateOpen) {
                return Collections.emptyList();
            }
            return delegate.search(query);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Recommendation<ISnippet>> search(String query, int maxResults) {
        readLock.lock();
        try {
            if (!isOpen() || !delegateOpen) {
                return Collections.emptyList();
            }
            return delegate.search(query, maxResults);
        } finally {
            readLock.unlock();
        }
    }

    @Subscribe
    public void onEvent(SnippetRepositoryConfigurationChangedEvent e) throws IOException {
        close();
        open();
    }

    /**
     * Triggered when a snippet repository URL was changed (most likely in the a preference page).
     * <p>
     * Clients of this event should be an instance of {@link ISnippetRepository}. Other clients should have a look at
     * {@link SnippetRepositoryClosedEvent} and {@link SnippetRepositoryClosedEvent}. Clients of this event may consider
     * refreshing themselves whenever they receive this event. Clients get notified in a background process.
     */
    public static class SnippetRepositoryConfigurationChangedEvent {
    }

    /**
     * Triggered when the snippet repository was closed to inform clients that the snippet repository is currently not
     * available.
     */
    public static class SnippetRepositoryClosedEvent {
        private final ISnippetRepository repo;

        public SnippetRepositoryClosedEvent(ISnippetRepository repo) {
            this.repo = repo;
        }

        public ISnippetRepository getRepository() {
            return repo;
        }
    }

    /**
     * Triggered when the snippet repository was opened to inform clients that the snippet repository is available.
     * <p>
     * Clients of this event may consider refreshing themselves whenever they receive this event. Clients get notified
     * in a background process.
     */
    public static class SnippetRepositoryOpenedEvent {

        private final ISnippetRepository repo;

        public SnippetRepositoryOpenedEvent(ISnippetRepository repo) {
            this.repo = repo;
        }

        public ISnippetRepository getRepository() {
            return repo;
        }
    }

    /**
     * Triggered when a snippet was imported.
     */
    public static class SnippetRepositoryContentChangedEvent {
        private final ISnippetRepository repo;

        public SnippetRepositoryContentChangedEvent(ISnippetRepository repo) {
            this.repo = repo;
        }

        public ISnippetRepository getRepository() {
            return repo;
        }
    }

    @Override
    public String getRepositoryLocation() {
        readLock.lock();
        try {
            Preconditions.checkState(isOpen());
            if (!delegateOpen) {
                return ""; //$NON-NLS-1$
            }
            return delegate.getRepositoryLocation();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getId() {
        readLock.lock();
        try {
            return delegate.getId();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean hasSnippet(UUID uuid) {
        readLock.lock();
        try {
            if (!isOpen() || !delegateOpen) {
                return false;
            }
            return delegate.hasSnippet(uuid);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean delete(UUID uuid) throws IOException {
        readLock.lock();
        try {
            if (!isOpen() || !delegateOpen) {
                return false;
            }
            boolean deleted = delegate.delete(uuid);
            if (deleted) {
                bus.post(new SnippetRepositoryContentChangedEvent(this));
            }
            return deleted;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isDeleteSupported() {
        readLock.lock();
        try {
            if (!isOpen() || !delegateOpen) {
                return false;
            }
            return delegate.isDeleteSupported();
        } finally {
            readLock.unlock();
        }
    }

    private boolean isOpen() {
        return timesOpened > 0;
    }

    @Override
    public void importSnippet(ISnippet snippet) throws IOException {
        writeLock.lock();
        try {
            Preconditions.checkState(isOpen());
            delegate.importSnippet(snippet);
            bus.post(new SnippetRepositoryContentChangedEvent(this));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean isImportSupported() {
        readLock.lock();
        try {
            if (!isOpen() || !delegateOpen) {
                return false;
            }
            return delegate.isImportSupported();
        } finally {
            readLock.unlock();
        }
    }

    public static ISnippetRepository createRepositoryInstance(EclipseGitSnippetRepositoryConfiguration config) {
        EventBus bus = InjectionService.getInstance().requestInstance(EventBus.class);
        File basedir = InjectionService.getInstance().requestAnnotatedInstance(File.class,
                Names.named(SnipmatchRcpModule.SNIPPET_REPOSITORY_BASEDIR));

        return new EclipseGitSnippetRepository(config.getId(), basedir, config.getUrl(), config.getPushUrl(),
                config.getPushBranchPrefix(), bus);
    }

    public static BasicEList<SnippetRepositoryConfiguration> getDefaultConfiguration() {
        BasicEList<SnippetRepositoryConfiguration> result = new BasicEList<SnippetRepositoryConfiguration>();

        EclipseGitSnippetRepositoryConfiguration configuration = SnipmatchRcpModelFactory.eINSTANCE
                .createEclipseGitSnippetRepositoryConfiguration();
        configuration.setName(Messages.DEFAULT_REPO_NAME);
        configuration.setDescription(Messages.ECLIPSE_GIT_SNIPPET_REPOSITORY_CONFIGURATION_DESCRIPTION);
        configuration.setEnabled(true);
        configuration
                .setUrl("https://git.eclipse.org/gitroot/recommenders/org.eclipse.recommenders.snipmatch.snippets.git"); //$NON-NLS-1$
        configuration
                .setPushUrl("https://git.eclipse.org/r/recommenders/org.eclipse.recommenders.snipmatch.snippets.git"); //$NON-NLS-1$
        configuration.setPushBranchPrefix("refs/for"); //$NON-NLS-1$

        result.add(configuration);
        return result;
    }

    @Override
    public boolean delete() {
        writeLock.lock();
        try {
            try {
                close();
                delegate.delete();
                return true;
            } catch (IOException e) {
                LOG.error("Exception while deleting files on disk.", e); //$NON-NLS-1$
                return false;
            }

        } finally {
            writeLock.unlock();
        }
    }
}
