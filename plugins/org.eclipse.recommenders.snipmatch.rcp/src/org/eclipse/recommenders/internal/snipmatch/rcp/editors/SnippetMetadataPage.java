/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Stefan Prisca - initial API and implementation
 *      Marcel Bruch - changed to use jface databinding
 *      Olav Lenz - clean up metadata page.
 */
package org.eclipse.recommenders.internal.snipmatch.rcp.editors;

import static org.eclipse.core.databinding.beans.PojoProperties.value;
import static org.eclipse.jface.databinding.swt.WidgetProperties.*;
import static org.eclipse.jface.databinding.viewers.ViewerProperties.singleSelection;

import java.util.UUID;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.internal.databinding.property.value.SelfValueProperty;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.recommenders.rcp.utils.ObjectToBooleanConverter;
import org.eclipse.recommenders.rcp.utils.Selections;
import org.eclipse.recommenders.snipmatch.ISnippet;
import org.eclipse.recommenders.snipmatch.Snippet;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import com.google.common.base.Optional;

@SuppressWarnings("restriction")
public class SnippetMetadataPage extends FormPage {

    private ISnippet snippet;

    private Text txtName;
    private Text txtDescription;
    private Text txtUuid;

    private ListViewer listViewer;
    private Composite btnContainer;
    private Button btnAddKeyword;
    private Button btnRemoveKeyword;

    private IObservableList ppKeywords;
    private DataBindingContext ctx;

    public SnippetMetadataPage(FormEditor editor, String id, String title) {
        super(editor, id, title);
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText("Metadata");
        Composite body = form.getBody();
        toolkit.decorateFormHeading(form.getForm());
        toolkit.paintBordersFor(body);
        managedForm.getForm().getBody().setLayout(new GridLayout(3, false));

        Label lblName = managedForm.getToolkit().createLabel(managedForm.getForm().getBody(), "Name:", SWT.NONE);
        lblName.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));

        txtName = managedForm.getToolkit().createText(managedForm.getForm().getBody(), null, SWT.NONE);
        txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Label lblDescription = managedForm.getToolkit().createLabel(managedForm.getForm().getBody(), "Description:",
                SWT.NONE);
        lblDescription.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

        txtDescription = managedForm.getToolkit().createText(managedForm.getForm().getBody(), null, SWT.NONE);
        txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Label lblKeyword = managedForm.getToolkit().createLabel(managedForm.getForm().getBody(), "Keywords:", SWT.NONE);
        lblKeyword.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));

        listViewer = new ListViewer(managedForm.getForm().getBody(), SWT.BORDER | SWT.V_SCROLL);
        List lstAliases = listViewer.getList();
        lstAliases.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        btnContainer = managedForm.getToolkit().createComposite(managedForm.getForm().getBody(), SWT.NONE);
        btnContainer.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1));
        managedForm.getToolkit().paintBordersFor(btnContainer);
        btnContainer.setLayout(new GridLayout(1, false));

        btnAddKeyword = managedForm.getToolkit().createButton(btnContainer, "Add...", SWT.NONE);
        btnAddKeyword.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new InputDialog(btnContainer.getShell(), "Enter new keyword", "Enter a new keyword", "", null) {
                    @Override
                    protected void okPressed() {
                        ppKeywords.add(getValue());
                        super.okPressed();
                    }
                }.open();
            }
        });
        btnAddKeyword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        btnRemoveKeyword = managedForm.getToolkit().createButton(btnContainer, "Remove", SWT.NONE);
        btnRemoveKeyword.setEnabled(false);
        btnRemoveKeyword.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Optional<String> o = Selections.getFirstSelected(listViewer);
                if (o.isPresent()) {
                    listViewer.remove(o.get());
                }
            }
        });
        btnRemoveKeyword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        Label lblUuid = managedForm.getToolkit().createLabel(managedForm.getForm().getBody(), "UUID:", SWT.NONE);
        lblUuid.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));

        txtUuid = managedForm.getToolkit().createText(managedForm.getForm().getBody(), null, SWT.READ_ONLY);
        txtUuid.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        initDataBindings();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        snippet = ((SnippetEditorInput) input).getSnippet();
        super.init(site, input);
    }

    @Override
    public void dispose() {
        super.dispose();
        // TODO: ctx is sometimes null. this is a workaround, see that ctx is
        // always initialized.
        if (ctx != null) {
            ctx.dispose();
        }
    }

    protected void initDataBindings() {
        ctx = new DataBindingContext();

        // name
        IObservableValue wpTxtNameText = text(SWT.Modify).observe(txtName);
        IObservableValue ppName = value(Snippet.class, "name", String.class).observe(snippet);
        ctx.bindValue(wpTxtNameText, ppName, null, null);

        // description
        IObservableValue wpTxtDescriptionText = text(SWT.Modify).observe(txtDescription);
        IObservableValue ppDescription = value(Snippet.class, "description", String.class).observe(snippet);
        ctx.bindValue(wpTxtDescriptionText, ppDescription, null, null);

        // keywords
        ppKeywords = PojoProperties.list(Snippet.class, "keywords", String.class).observe(snippet);
        ViewerSupport.bind(listViewer, ppKeywords, new SelfValueProperty(String.class));

        // uuid
        IObservableValue wpUuidText = text(SWT.Modify).observe(txtUuid);
        IObservableValue ppUuid = value(Snippet.class, "uuid", UUID.class).observe(snippet);
        ctx.bindValue(wpUuidText, ppUuid, null, null);

        // button enablement
        IObservableValue vpKeywordSelection = singleSelection().observe(listViewer);
        IObservableValue wpBtnRemoveKeywordsEnable = enabled().observe(btnRemoveKeyword);

        UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setConverter(new ObjectToBooleanConverter());
        ctx.bindValue(vpKeywordSelection, wpBtnRemoveKeywordsEnable, strategy, null);

        for (Object o : ctx.getValidationStatusProviders()) {
            if (o instanceof Binding) {
                ((Binding) o).getTarget().addChangeListener(new IChangeListener() {

                    @Override
                    public void handleChange(org.eclipse.core.databinding.observable.ChangeEvent event) {
                        ((SnippetEditor) getEditor()).setDirty(true);
                    }
                });
            }
        }
    }
}
