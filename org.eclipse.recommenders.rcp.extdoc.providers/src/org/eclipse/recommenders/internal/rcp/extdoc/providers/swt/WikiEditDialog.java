/**
 * Copyright (c) 2011 Stefan Henss.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Henss - initial API and implementation.
 */
package org.eclipse.recommenders.internal.rcp.extdoc.providers.swt;

import com.google.common.base.Preconditions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.recommenders.internal.rcp.extdoc.providers.ExtDocProvidersPlugin;
import org.eclipse.recommenders.internal.rcp.extdoc.providers.WikiProvider;
import org.eclipse.recommenders.rcp.extdoc.AbstractDialog;
import org.eclipse.recommenders.rcp.extdoc.SwtFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public final class WikiEditDialog extends AbstractDialog {

    private final WikiProvider provider;
    private Text text;
    private final IJavaElement javaElement;

    private final String editContent;

    public WikiEditDialog(final WikiProvider provider, final IJavaElement javaElement, final String editContent) {
        super(provider.getShell());
        setBlockOnOpen(false);

        this.provider = provider;
        this.javaElement = javaElement;

        if (editContent == null) {
            this.editContent = "Please enter text ...";
        } else {
            this.editContent = editContent;
        }
    }

    @Override
    protected void contentsCreated() {
        setOkButtonText("Save");
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        setTitle(String.format("Edit Wiki for %s", javaElement.getElementName()));
        setMessage("Bla Bla");
        final Image image = AbstractUIPlugin.imageDescriptorFromPlugin(
                ExtDocProvidersPlugin.getDefault().getBundle().getSymbolicName(), "icons/full/wizban/edit.png")
                .createImage();
        setTitleImage(image);

        final Composite composite = (Composite) super.createDialogArea(parent);
        final Composite area = SwtFactory.createGridComposite(composite, 1, 0, 10, 15, 20);
        new Label(area, SWT.NONE).setText("Wiki source (Textile, see help button):");
        text = SwtFactory.createText(area, editContent, 350, 500);
        SwtFactory.createCheck(area, "Add my name (...) as author instead of uploading anonymously.", true);
        SwtFactory.createSeparator(composite);
        return composite;
    }

    @Override
    protected void okPressed() {
        try {
            if (!text.getText().equals(editContent)) {
                provider.update(javaElement, text.getText());
            }
        } finally {
            Preconditions.checkArgument(close());
        }
    }

}
