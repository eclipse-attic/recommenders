/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Olav Lenz - initial API and implementation.
 */
package org.eclipse.recommenders.internal.models.rcp;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.eclipse.recommenders.internal.models.rcp.Messages.PREFPAGE_ADVISOR_ADVISORS;
import static org.eclipse.recommenders.internal.models.rcp.Messages.PREFPAGE_ADVISOR_BUTTON_DOWN;
import static org.eclipse.recommenders.internal.models.rcp.Messages.PREFPAGE_ADVISOR_BUTTON_UP;
import static org.eclipse.recommenders.internal.models.rcp.Messages.PREFPAGE_ADVISOR_DESCRIPTION;
import static org.eclipse.recommenders.internal.models.rcp.Messages.PREFPAGE_ADVISOR_TITLE;
import static org.eclipse.recommenders.utils.Checks.cast;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.recommenders.models.IProjectCoordinateAdvisor;
import org.eclipse.recommenders.models.advisors.ProjectCoordinateAdvisorService;
import org.eclipse.recommenders.models.rcp.ModelEvents.AdvisorConfigurationChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

public class AdvisorsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final int UP = -1;
    private static final int DOWN = +1;

    private final EventBus bus;
    private final ProjectCoordinateAdvisorService advisorService;

    @Inject
    public AdvisorsPreferencePage(EventBus bus, ProjectCoordinateAdvisorService advisorService) {
        super(GRID);
        this.bus = bus;
        this.advisorService = advisorService;
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, Constants.BUNDLE_ID));
        setMessage(PREFPAGE_ADVISOR_TITLE);
        setDescription(PREFPAGE_ADVISOR_DESCRIPTION);
    }

    @Override
    protected void createFieldEditors() {
        addField(new AdvisorEditor(Constants.P_ADVISOR_LIST_SORTED, PREFPAGE_ADVISOR_ADVISORS, getFieldEditorParent()));
    }

    private final class AdvisorEditor extends FieldEditor {

        private CheckboxTableViewer tableViewer;
        private Composite buttonBox;
        private Button upButton;
        private Button downButton;

        private AdvisorEditor(String name, String labelText, Composite parent) {
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
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).span(numColumns - 1, 1).applyTo(tableViewer.getTable());
            tableViewer.getTable().addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateButtonStatus();
                }
            });

            buttonBox = getButtonControl(parent);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).applyTo(buttonBox);
        }

        private void updateButtonStatus() {
            int selectionIndex = tableViewer.getTable().getSelectionIndex();
            upButton.setEnabled(selectionIndex != -1 && selectionIndex > 0);
            downButton.setEnabled(selectionIndex != -1 && selectionIndex < tableViewer.getTable().getItemCount() - 1);
        }

        private final class MoveSelectionListener extends SelectionAdapter {

            private final int direction;

            public MoveSelectionListener(int direction) {
                this.direction = direction;
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                List<AdvisorDescriptor> input = cast(tableViewer.getInput());
                int index = tableViewer.getTable().getSelectionIndex();
                AdvisorDescriptor movedElement = input.remove(index);
                int newIndex = min(max(0, index + direction), input.size());
                input.add(newIndex, movedElement);
                tableViewer.setInput(input);
                updateButtonStatus();
            }
        }

        private Composite getButtonControl(Composite parent) {
            Composite box = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().applyTo(box);

            upButton = new Button(box, SWT.PUSH);
            upButton.setText(PREFPAGE_ADVISOR_BUTTON_UP);
            upButton.setEnabled(false);
            upButton.addSelectionListener(new MoveSelectionListener(UP));
            GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(upButton);

            downButton = new Button(box, SWT.PUSH);
            downButton.setText(PREFPAGE_ADVISOR_BUTTON_DOWN);
            downButton.setEnabled(false);
            downButton.addSelectionListener(new MoveSelectionListener(DOWN));
            GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(downButton);

            return box;
        }

        private CheckboxTableViewer getTableControl(Composite parent) {
            CheckboxTableViewer tableViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION);
            tableViewer.setLabelProvider(new ColumnLabelProvider() {

                @Override
                public String getText(Object element) {
                    AdvisorDescriptor descriptor = cast(element);
                    return descriptor.getName();
                }
                
                @Override
                public String getToolTipText(Object element) {
                    AdvisorDescriptor descriptor = cast(element);
                    return descriptor.getDescription();
                }
            });
            ColumnViewerToolTipSupport.enableFor(tableViewer); 
            tableViewer.setContentProvider(new ArrayContentProvider());
            return tableViewer;
        }

        @Override
        protected void doLoad() {
            String value = getPreferenceStore().getString(getPreferenceName());
            load(value);
        }

        private void load(String value) {
            List<AdvisorDescriptor> input = AdvisorDescriptors.load(value, AdvisorDescriptors.getRegisteredAdvisors());
            List<AdvisorDescriptor> checkedElements = Lists.newArrayList();
            for (AdvisorDescriptor descriptor :input) {
                if (descriptor.isEnabled()) {
                    checkedElements.add(descriptor);
                }
            }

            tableViewer.setInput(input);
            tableViewer.setCheckedElements(checkedElements.toArray());
        }

        @Override
        protected void doLoadDefault() {
            String value = getPreferenceStore().getDefaultString(getPreferenceName());
            load(value);
        }

        @Override
        protected void doStore() {
            List<AdvisorDescriptor> descriptors = cast(tableViewer.getInput());
            for (AdvisorDescriptor descriptor : descriptors) {
                descriptor.setEnabled(tableViewer.getChecked(descriptor));
            }
            String newValue = AdvisorDescriptors.store(descriptors);
            getPreferenceStore().setValue(getPreferenceName(), newValue);
            reconfigureAdvisorService(descriptors);
        }

        private void reconfigureAdvisorService(List<AdvisorDescriptor> descriptors) {
            List<IProjectCoordinateAdvisor> advisors = Lists.newArrayListWithCapacity(descriptors.size());
            for (AdvisorDescriptor descriptor : descriptors) {
                try {
                    if (descriptor.isEnabled()) {
                        advisors.add(descriptor.createAdvisor());
                    }
                } catch (CoreException e) {
                    continue; // skip
                }
            }
            advisorService.setAdvisors(advisors);
            bus.post(new AdvisorConfigurationChangedEvent());
        }

        @Override
        public int getNumberOfControls() {
            return 2;
        }
    }
}
