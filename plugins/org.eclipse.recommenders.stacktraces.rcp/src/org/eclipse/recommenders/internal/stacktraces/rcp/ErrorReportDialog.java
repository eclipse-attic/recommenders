/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Daniel Haftstein - added support for multiple stacktraces
 */
package org.eclipse.recommenders.internal.stacktraces.rcp;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.recommenders.internal.stacktraces.rcp.model.ErrorReport;
import org.eclipse.recommenders.internal.stacktraces.rcp.model.ErrorReports;
import org.eclipse.recommenders.internal.stacktraces.rcp.model.SendAction;
import org.eclipse.recommenders.internal.stacktraces.rcp.model.Settings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

public class ErrorReportDialog extends MessageDialog {

    public static ImageDescriptor TITLE_IMAGE_DESC = ImageDescriptor.createFromFile(ErrorReportDialog.class,
            "/icons/wizban/stackframes_wiz.gif");
    private static final Image ERROR_ICON = PlatformUI.getWorkbench().getSharedImages()
            .getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
    private Settings settings;
    private TableViewer tableViewer;
    private StyledText messageText;
    private IObservableList errors;
    private ErrorReport activeSelection;

    private Text commentText;
    private Button disableButton;
    private ComboViewer rememberSettingCombo;

    public ErrorReportDialog(Shell parentShell, Settings settings, IObservableList errors) {
        super(
                parentShell,
                "An Error Was Logged",
                TITLE_IMAGE_DESC.createImage(),
                "We noticed a new error event was logged. Such error events may reveal issues in the Eclipse codebase, and thus we kindly ask you to report it to eclipse.org.",
                MessageDialog.WARNING, new String[] { "Send", "Cancel" }, 0);
        setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.APPLICATION_MODAL);
        this.settings = settings;
        this.errors = errors;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setLayout(new GridLayout(1, false));
    }

    @Override
    public Composite createCustomArea(final Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
        GridLayoutFactory.fillDefaults().applyTo(container);

        createCommentContainer(parent, container);

        createDetailsContainer(parent, container);

        Composite settingsComposite = createSettingsComposite(parent);
        GridDataFactory.fillDefaults().grab(true, false).indent(0, 5).align(SWT.BEGINNING, SWT.CENTER)
                .applyTo(settingsComposite);

        return container;
    }

    protected void createCommentContainer(final Composite parent, Composite container) {
        final ExpandableComposite commentContainer = new ExpandableComposite(container, SWT.NONE);
        commentContainer.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        commentContainer.setText("Comments");
        commentContainer.setExpanded(true);

        Composite commentContent = new Composite(commentContainer, SWT.NONE);
        GridLayoutFactory.fillDefaults().applyTo(commentContent);

        commentText = new Text(commentContent, SWT.BORDER | SWT.MULTI);
        commentText.setMessage("Optional. Please provide additional information "
                + "like steps that allow committers to reproduce this error.");
        GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 50).grab(true, false).applyTo(commentText);
        commentContainer.setClient(commentContent);
        commentContainer.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                parent.getShell().pack();
            }
        });
    }

    protected void createDetailsContainer(final Composite parent, Composite container) {
        ExpandableComposite detailsContainer = new ExpandableComposite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(detailsContainer);

        detailsContainer.setText("Details");
        Composite detailsContent = createDetailsContent(detailsContainer);
        detailsContainer.setClient(detailsContent);
        detailsContainer.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                parent.getShell().pack();
            }
        });
    }

    private Composite createDetailsContent(Composite container) {
        SashForm sash = new SashForm(container, SWT.HORIZONTAL);
        createTableComposite(sash);
        createMessageComposite(sash);
        sash.setWeights(new int[] { 20, 80 });
        return sash;
    }

    private Composite createTableComposite(Composite container) {
        Composite tableComposite = new Composite(container, SWT.NONE);
        tableViewer = new TableViewer(tableComposite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION
                | SWT.BORDER);
        TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
        column.setLabelProvider(new ColumnLabelProvider() {

            @Override
            public String getText(Object element) {
                ErrorReport event = (ErrorReport) element;
                return event.getStatus().getMessage();
            }

            @Override
            public Image getImage(Object element) {
                return ERROR_ICON;
            }
        });
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        tableColumnLayout.setColumnData(column.getColumn(), new ColumnWeightData(100));
        tableComposite.setLayout(tableColumnLayout);
        tableViewer.setContentProvider(new ObservableListContentProvider());
        tableViewer.setInput(errors);
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                if (!event.getSelection().isEmpty()) {
                    IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
                    activeSelection = (ErrorReport) selection.getFirstElement();
                    ErrorReport copy = ErrorReports.copy(activeSelection);
                    copy.setName(settings.getName());
                    copy.setEmail(settings.getEmail());
                    messageText.setText(ErrorReports.prettyPrint(copy, settings));
                    String comment = activeSelection.getComment();
                    commentText.setText(comment == null ? "" : comment);
                }
            }

        });
        return tableComposite;
    }

    private Composite createMessageComposite(Composite container) {
        Composite messageComposite = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().applyTo(messageComposite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(messageComposite);
        messageText = new StyledText(messageComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        messageText.setEditable(false);
        messageText.setMargins(5, messageText.getTopMargin(), messageText.getRightMargin(),
                messageText.getBottomMargin());
        messageText.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        messageText.setForeground(new Color(container.getDisplay(), 80, 80, 80));
        GridDataFactory.fillDefaults().minSize(100, 1).hint(SWT.DEFAULT, 300).grab(true, true).applyTo(messageText);
        return messageComposite;
    }

    protected Composite createSettingsComposite(final Composite parent) {
        Composite settingsComposite = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(settingsComposite);
        disableButton = new Button(settingsComposite, SWT.CHECK);
        disableButton.setText("I want to disable error reporting");
        rememberSettingCombo = new ComboViewer(settingsComposite);
        rememberSettingCombo.setContentProvider(ArrayContentProvider.getInstance());
        rememberSettingCombo.getCombo().setEnabled(false);

        rememberSettingCombo.setInput(new SendAction[] { SendAction.IGNORE, SendAction.PAUSE_RESTART,
                SendAction.PAUSE_DAY });
        rememberSettingCombo.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                SendAction mode = (SendAction) element;
                switch (mode) {
                case IGNORE:
                    return "permanently";
                case PAUSE_RESTART:
                    return "until Restart (Pause)";
                case PAUSE_DAY:
                    return "for 24 hours (Pause)";
                default:
                    return super.getText(element);
                }
            }
        });
        rememberSettingCombo.setSelection(new StructuredSelection(SendAction.IGNORE));
        disableButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                rememberSettingCombo.getCombo().setEnabled(disableButton.getSelection());
                Button okButton = getButton(0);
                if (disableButton.getSelection()) {
                    okButton.setText("Ok");
                } else {
                    okButton.setText("Send");
                }
            }
        });
        return settingsComposite;
    }

    @Override
    protected void okPressed() {
        if (disableButton.getSelection()) {
            // /if disable is selected, the "ok" is a cancel from the view of sending a report.
            setReturnCode(CANCEL);
            SendAction action = (SendAction) rememberSettingCombo.getStructuredSelection().getFirstElement();
            settings.setAction(action);
            PreferenceInitializer.saveSettings(settings);
        } else {
            setReturnCode(OK);
        }

        close();
    }

    @Override
    public void create() {
        super.create();
        if (!errors.isEmpty()) {
            StructuredSelection selection = new StructuredSelection(tableViewer.getElementAt(0));
            tableViewer.setSelection(selection, true);
        }
    }
}
