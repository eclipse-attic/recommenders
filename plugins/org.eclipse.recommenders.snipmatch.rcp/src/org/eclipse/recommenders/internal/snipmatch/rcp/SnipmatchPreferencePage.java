/**
 * Copyright (c) 2013 Madhuranga Lakjeewa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Madhuranga Lakjeewa - initial API and implementation.
 *    Olav Lenz - introduce ISnippetRepositoryConfiguration.
 */
package org.eclipse.recommenders.internal.snipmatch.rcp;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.recommenders.internal.snipmatch.rcp.Constants.PREF_SNIPPETS_REPO;
import static org.eclipse.recommenders.utils.Checks.cast;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.window.Window;
import org.eclipse.recommenders.snipmatch.ISnippetRepositoryConfiguration;
import org.eclipse.recommenders.snipmatch.ISnippetRepositoryProvider;
import org.eclipse.recommenders.utils.Checks;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SnipmatchPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private ImmutableSet<ISnippetRepositoryProvider> providers;

    @Inject
    public SnipmatchPreferencePage(
            @Named(SnipmatchRcpModule.SNIPPET_REPOSITORY_PROVIDERS) ImmutableSet<ISnippetRepositoryProvider> providers) {
        super(GRID);
        this.providers = providers;
        setDescription(Messages.PREFPAGE_DESCRIPTION);
    }

    @Override
    public void createFieldEditors() {
        ConfigurationEditor configurationEditor = new ConfigurationEditor(PREF_SNIPPETS_REPO,
                Messages.PREFPAGE_LABEL_REMOTE_SNIPPETS_REPOSITORY, getFieldEditorParent());
        addField(configurationEditor);
    }

    @Override
    public void init(IWorkbench workbench) {
        ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, Constants.BUNDLE_ID);
        setPreferenceStore(store);
    }

    private final class ConfigurationEditor extends FieldEditor {

        private CheckboxTableViewer tableViewer;

        private Composite buttonBox;
        private Button newButton;
        private Button editButton;
        private Button removeButton;

        private ConfigurationEditor(String name, String labelText, Composite parent) {
            super(name, labelText, parent);
        }

        @Override
        protected void adjustForNumColumns(int numColumns) {
        }

        @Override
        protected void doFillIntoGrid(Composite parent, int numColumns) {
            Control control = getLabelControl(parent);
            GridDataFactory.swtDefaults().span(numColumns, 1).applyTo(control);

            tableViewer = getTableControl(parent);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).span(numColumns - 1, 1).grab(true, true)
                    .applyTo(tableViewer.getTable());
            tableViewer.getTable().addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateButtonStatus();
                }
            });

            buttonBox = getButtonControl(parent);
            updateButtonStatus();
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(buttonBox);
        }

        private void updateButtonStatus() {
            boolean selected = tableViewer.getTable().getSelectionIndex() != -1;
            boolean editableType = getSelectedConfiguration() instanceof EclipseGitSnippetRepositoryConfiguration;
            editButton.setEnabled(selected && editableType);
            removeButton.setEnabled(selected);
        }

        private Composite getButtonControl(Composite parent) {
            Composite box = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().applyTo(box);

            newButton = createButton(box, Messages.PREFPAGE_BUTTON_NEW);
            newButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    addNewConfiguration();
                    updateButtonStatus();
                }

            });

            editButton = createButton(box, Messages.PREFPAGE_BUTTON_EDIT);
            editButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    editConfiguration(getSelectedConfiguration());
                    updateButtonStatus();
                }

            });

            editButton.setEnabled(false);

            removeButton = createButton(box, Messages.PREFPAGE_BUTTON_REMOVE);
            removeButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    removeConfiguration(getSelectedConfiguration());
                    updateButtonStatus();
                }

            });

            return box;
        }

        private ISnippetRepositoryConfiguration getSelectedConfiguration() {
            ISnippetRepositoryConfiguration configuration = cast(tableViewer.getElementAt(tableViewer.getTable()
                    .getSelectionIndex()));
            return configuration;
        }

        protected void removeConfiguration(ISnippetRepositoryConfiguration configuration) {
            List<ISnippetRepositoryConfiguration> configurations = cast(tableViewer.getInput());
            configurations.remove(configuration);
            tableViewer.setInput(configurations);
        }

        protected void editConfiguration(ISnippetRepositoryConfiguration configuration) {
            Checks.ensureIsTrue(configuration instanceof EclipseGitSnippetRepositoryConfiguration);
            EclipseGitSnippetRepositoryConfiguration oldConfig = cast(configuration);

            String name = showDialogForName(oldConfig.getName());
            if (isNullOrEmpty(name)) {
                return;
            }
            String repositoryUrl = showDialogForRepositoryUrl(oldConfig.getRepositoryUrl());
            if (isNullOrEmpty(repositoryUrl)) {
                return;
            }

            EclipseGitSnippetRepositoryConfiguration newConfig = new EclipseGitSnippetRepositoryConfiguration(name,
                    repositoryUrl, oldConfig.isEnabled());

            List<ISnippetRepositoryConfiguration> configurations = cast(tableViewer.getInput());
            configurations.remove(oldConfig);
            configurations.add(newConfig);
            updateTableContent(configurations);
        }

        private String showDialogForRepositoryUrl(String initialValue) {
            InputDialog d = new InputDialog(getShell(), Messages.DIALOG_TITLE_SET_SNIPPET_REPOSITORY_URL,
                    Messages.DIALOG_MESSAGE_SET_SNIPPET_REPOSITORY_URL, initialValue, new UriInputValidator());
            if (d.open() == Window.OK) {
                return d.getValue();
            }
            return null;
        }

        private String showDialogForName(String initialValue) {
            InputDialog d = new InputDialog(getShell(), Messages.DIALOG_TITLE_CHANGE_CONFIGURATION_NAME,
                    Messages.DIALOG_MESSAGE_CHANGE_CONFIGURATION_NAME, initialValue, null);
            if (d.open() == Window.OK) {
                return d.getValue();
            }
            return null;
        }

        protected void addNewConfiguration() {
            String name = showDialogForName("");
            if (isNullOrEmpty(name)) {
                return;
            }

            String repositoryUrl = showDialogForRepositoryUrl("");
            if (isNullOrEmpty(repositoryUrl)) {
                return;
            }

            EclipseGitSnippetRepositoryConfiguration newConfig = new EclipseGitSnippetRepositoryConfiguration(name,
                    repositoryUrl, true);
            List<ISnippetRepositoryConfiguration> configurations = cast(tableViewer.getInput());
            configurations.add(newConfig);
            tableViewer.setInput(configurations);
        }

        private final class UriInputValidator implements IInputValidator {
            @Override
            public String isValid(String newText) {
                // TODO this does not support git:// urls
                try {
                    new URI(newText);
                    return null;
                } catch (URISyntaxException e) {
                    return e.getMessage();
                }
            }
        }

        private Button createButton(Composite box, String text) {
            Button button = new Button(box, SWT.PUSH);
            button.setText(text);

            int widthHint = Math.max(convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH),
                    button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);

            GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).hint(widthHint, SWT.DEFAULT).applyTo(button);

            return button;
        }

        private CheckboxTableViewer getTableControl(Composite parent) {
            final CheckboxTableViewer tableViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER
                    | SWT.FULL_SELECTION);

            tableViewer.setLabelProvider(new ColumnLabelProvider() {

                @Override
                public String getText(Object element) {
                    ISnippetRepositoryConfiguration config = cast(element);
                    return config.getName();
                }

                @Override
                public String getToolTipText(Object element) {
                    ISnippetRepositoryConfiguration config = cast(element);
                    StringBuilder sb = new StringBuilder();
                    sb.append(config.getDescription()).append(System.lineSeparator());

                    for (Iterator<Entry<String, String>> iter = config.getAttributes().entrySet().iterator(); iter
                            .hasNext();) {
                        Entry<String, String> entry = iter.next();
                        sb.append(MessageFormat.format(Messages.CONFIGURATION_DISPLAY_STRING, entry.getKey(),
                                entry.getValue()));
                        if (iter.hasNext()) {
                            sb.append(System.lineSeparator());
                        }
                    }
                    return sb.toString();
                }
            });
            ColumnViewerToolTipSupport.enableFor(tableViewer);
            tableViewer.setContentProvider(new ArrayContentProvider());
            return tableViewer;
        }

        @Override
        protected void doLoad() {
            updateTableContent(readConfigurations(getPreferenceStore().getString(getPreferenceName())));
        }

        private List<ISnippetRepositoryConfiguration> readConfigurations(String stringRepresentation) {
            return Lists.newArrayList(RepositoryConfigurations.fromPreferenceString(stringRepresentation, providers));
        }

        public void updateTableContent(List<ISnippetRepositoryConfiguration> configurations) {
            Collection<ISnippetRepositoryConfiguration> checkedConfigurations = Collections2.filter(configurations,
                    new Predicate<ISnippetRepositoryConfiguration>() {

                        @Override
                        public boolean apply(ISnippetRepositoryConfiguration input) {
                            return input.isEnabled();
                        }

                    });

            tableViewer.setInput(configurations);
            tableViewer.setCheckedElements(checkedConfigurations.toArray());
        }

        @Override
        protected void doLoadDefault() {
            updateTableContent(readConfigurations(getPreferenceStore().getDefaultString(getPreferenceName())));
        }

        @Override
        protected void doStore() {
            List<ISnippetRepositoryConfiguration> configurations = cast(tableViewer.getInput());
            for (ISnippetRepositoryConfiguration configuration : configurations) {
                configuration.setEnabled(tableViewer.getChecked(configuration));
            }
            String preferenceString = RepositoryConfigurations.toPreferenceString(configurations, providers);
            getPreferenceStore().setValue(getPreferenceName(), preferenceString);
        }

        @Override
        public int getNumberOfControls() {
            return 2;
        }
    }

}
