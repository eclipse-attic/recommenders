/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Daniel Haftstein - added UI thread safety
 */
package org.eclipse.recommenders.internal.stacktraces.rcp;

import static org.eclipse.recommenders.internal.stacktraces.rcp.Constants.*;
import static org.eclipse.recommenders.internal.stacktraces.rcp.model.ErrorReports.newErrorReport;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.recommenders.internal.stacktraces.rcp.model.ErrorReport;
import org.eclipse.recommenders.internal.stacktraces.rcp.model.SendAction;
import org.eclipse.recommenders.internal.stacktraces.rcp.model.Settings;
import org.eclipse.recommenders.utils.Checks;
import org.eclipse.recommenders.utils.Logs;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;

public class LogListener implements ILogListener, IStartup {

    private Cache<String, ErrorReport> cache = CacheBuilder.newBuilder()
            .maximumSize(Constants.PREVIOUS_ERROR_CACHE_MAXIMUM_SIZE)
            .expireAfterAccess(Constants.PREVIOUS_ERROR_CACHE_EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES).build();
    private IObservableList errorReports;
    private volatile boolean isDialogOpen;
    private Settings settings;
    private StandInStacktraceProvider stacktraceProvider = new StandInStacktraceProvider();

    public LogListener() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                errorReports = Properties.selfList(ErrorReport.class).observe(Lists.newArrayList());
            }
        });
    }

    @Override
    public void earlyStartup() {
        Platform.addLogListener(this);
    }

    @Override
    public void logging(final IStatus status, String nouse) {
        try {
            if (!isReportingAllowedInEnvironment() || !isErrorSeverity(status) || isWorkbenchClosing()) {
                return;
            }
            settings = readSettings();
            if (!settings.isConfigured()) {
                firstConfiguration();
            }
            if (!settings.isConfigured()) {
                Logs.log(LogMessages.FIRST_CONFIGURATION_FAILED);
                return;
            }
            if (!hasPluginIdWhitelistedPrefix(status, settings.getWhitelistedPluginIds())) {
                return;
            }
            SendAction sendAction = settings.getAction();
            if (!isSendingAllowedOnAction(sendAction)) {
                return;
            }
            final ErrorReport report = newErrorReport(status, settings);
            stacktraceProvider.insertStandInStacktraceIfEmpty(report.getStatus());
            if (settings.isSkipSimilarErrors() && sentSimilarErrorBefore(report)) {
                return;
            }
            addForSending(report);
            if (sendAction == SendAction.ASK) {
                checkAndSendWithDialog(report);
            } else if (sendAction == SendAction.SILENT) {
                sendAndClear();
            }
        } catch (Exception e) {
            Logs.log(LogMessages.REPORTING_ERROR, e);
        }
    }

    private boolean isWorkbenchClosing() {
        return PlatformUI.getWorkbench().isClosing();
    }

    private boolean isReportingAllowedInEnvironment() {
        return !skipSendingReports() && !isRuntimeEclipse();
    }

    private void sendAndClear() {
        sendList();
        clear();
    }

    private void sendList() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                for (Object entry : errorReports) {
                    ErrorReport report = Checks.cast(entry);
                    sendStatus(report);
                }
            }
        });
    }

    private void clear() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                errorReports.clear();
            }
        });
    }

    private boolean skipSendingReports() {
        return Boolean.getBoolean(SYSPROP_SKIP_REPORTS);
    }

    private boolean isRuntimeEclipse() {
        return null == System.getProperty(SYSPROP_ECLIPSE_BUILD_ID);
    }

    private boolean isErrorSeverity(final IStatus status) {
        return status.matches(IStatus.ERROR);
    }

    @VisibleForTesting
    protected Settings readSettings() {
        return PreferenceInitializer.readSettings();
    }

    private static boolean hasPluginIdWhitelistedPrefix(IStatus status, List<String> whitelistedIdPrefixes) {
        String pluginId = status.getPlugin();
        for (String id : whitelistedIdPrefixes) {
            if (pluginId.startsWith(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSendingAllowedOnAction(SendAction sendAction) {
        return sendAction == SendAction.ASK || sendAction == SendAction.SILENT;
    }

    private boolean sentSimilarErrorBefore(final ErrorReport report) {
        return cache.getIfPresent(computeCacheKey(report)) != null;
    }

    private void firstConfiguration() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                Optional<Shell> shell = getWorkbenchWindowShell();
                if (shell.isPresent()) {
                    Configurator.ConfigureWithDialog(settings, shell.get());
                    PreferenceInitializer.saveSettings(settings);
                }
            }
        });
    }

    private String computeCacheKey(final ErrorReport report) {
        return report.getStatus().getFingerprint();
    }

    private void addForSending(final ErrorReport report) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                errorReports.add(report);
            }
        };
        cache.put(computeCacheKey(report), report);
        Display current = Display.getCurrent();
        if (current != null) {
            run.run();
        } else {
            Display.getDefault().asyncExec(run);
        }
    }

    @VisibleForTesting
    protected void checkAndSendWithDialog(final ErrorReport report) {
        // run on UI-thread to ensure that the observable list is not modified from another thread
        // and that the wizard is created on the UI-thread.
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (isDialogOpen) {
                    return;
                }
                isDialogOpen = true;
                Optional<Shell> shell = getWorkbenchWindowShell();
                if (shell.isPresent()) {
                    ErrorReportDialog reportDialog = new ErrorReportDialog(shell.get(), settings, errorReports) {
                        @Override
                        public boolean close() {
                            boolean close = super.close();
                            if (close) {
                                isDialogOpen = false;
                            }
                            return close;
                        }
                    };
                    reportDialog.open();
                }
            }
        });
    }

    @VisibleForTesting
    protected void sendStatus(final ErrorReport report) {
        // double safety. This is checked before elsewhere. But just to make sure...
        if (settings.getAction() == SendAction.IGNORE) {
            return;
        }
        new UploadJob(report, settings, URI.create(settings.getServerUrl())).schedule();
    }

    private static Optional<Shell> getWorkbenchWindowShell() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow != null) {
            Shell shell = workbenchWindow.getShell();
            if (shell != null) {
                return Optional.of(shell);
            }
        }
        return Optional.absent();
    }
}
