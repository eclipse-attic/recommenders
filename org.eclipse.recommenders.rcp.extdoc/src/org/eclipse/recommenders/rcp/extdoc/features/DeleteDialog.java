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
package org.eclipse.recommenders.rcp.extdoc.features;

import org.eclipse.recommenders.rcp.extdoc.AbstractDialog;
import org.eclipse.recommenders.rcp.extdoc.IDeletionProvider;
import org.eclipse.recommenders.rcp.extdoc.SwtFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.google.common.base.Preconditions;

final class DeleteDialog extends AbstractDialog {

    private final IDeletionProvider provider;
    private final String objectName;
    private final Object object;

    DeleteDialog(final IDeletionProvider provider, final Object object, final String objectName) {
        super(provider.getShell());
        this.provider = provider;
        this.object = object;
        this.objectName = objectName;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        setTitle(String.format("Delete %s", objectName));
        setMessage("Are you sure?");
        setTitleImage("delete.png");

        final Composite composite = (Composite) super.createDialogArea(parent);
        final Composite area = SwtFactory.createGridComposite(composite, 1, 0, 10, 15, 20);
        createDialogContent(area);

        SwtFactory.createSeparator(composite);
        return composite;
    }

    private void createDialogContent(final Composite area) {
        SwtFactory.createLabel(area, "Are you sure to delete " + objectName + "?", true, false, SWT.COLOR_BLACK);
        SwtFactory.createLabel(area, "");
        SwtFactory.createCheck(area, "Do not display this item anymore.", true);
        SwtFactory.createCheck(area, "Send anonymous information about this deletion to provider as feedback.", false);
    }

    @Override
    protected void okPressed() {
        try {
            provider.requestDeletion(object);
        } finally {
            Preconditions.checkArgument(close());
        }
    }

}
