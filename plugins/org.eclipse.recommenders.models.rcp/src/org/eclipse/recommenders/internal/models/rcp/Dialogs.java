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

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.recommenders.internal.models.rcp.l10n.Messages;
import org.eclipse.recommenders.utils.Uris;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.collect.ImmutableList;

public final class Dialogs {

    private static final List<String> SUPPORTED_PROTOCOLS = ImmutableList.of("file", "http", "https"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private Dialogs() {
    }


    public static InputDialog newModelRepositoryUrlDialog(Shell parent, final String[] remoteUris) {
        return new InputDialog(parent, Messages.DIALOG_TITLE_ADD_MODEL_REPOSITORY, Messages.FIELD_LABEL_REPOSITORY_URI,
                "http://download.eclipse.org/recommenders/models/<version>", //$NON-NLS-1$
                new IInputValidator() {

                    @Override
                    public String isValid(String newText) {
                        URI uri = Uris.parseURI(newText).orNull();
                        if (uri == null) {
                            return Messages.DIALOG_MESSAGE_INVALID_URI;
                        }
                        if (!uri.isAbsolute()) {
                            return Messages.DIALOG_MESSAGE_NOT_ABSOLUTE_URI;
                        }
                        if (isUriAlreadyAdded(uri)) {
                            return Messages.DIALOG_MESSAGE_URI_ALREADY_ADDED;
                        }

                        if (!Uris.isUriProtocolSupported(uri, SUPPORTED_PROTOCOLS)) {
                            return MessageFormat.format(Messages.DIALOG_MESSAGE_UNSUPPORTED_PROTOCOL, uri.getScheme(),
                                    StringUtils.join(SUPPORTED_PROTOCOLS, Messages.LIST_SEPARATOR));
                        }
                        return null;
                    }

                    private boolean isUriAlreadyAdded(URI uri) {
                        String mangledUri = Uris.mangle(uri);
                        for (String remoteUri : remoteUris) {
                            if (Uris.mangle(Uris.toUri(remoteUri)).equals(mangledUri)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }
}
