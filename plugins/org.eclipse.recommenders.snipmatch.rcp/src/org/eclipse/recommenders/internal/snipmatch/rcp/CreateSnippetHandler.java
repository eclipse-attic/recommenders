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

import static org.eclipse.jface.dialogs.MessageDialog.openError;
import static org.eclipse.recommenders.internal.snipmatch.rcp.Constants.EDITOR_ID;
import static org.eclipse.recommenders.utils.Checks.cast;
import static org.eclipse.ui.handlers.HandlerUtil.getActiveWorkbenchWindow;

import java.util.Set;

import javax.inject.Inject;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.recommenders.snipmatch.ISnippetRepository;
import org.eclipse.recommenders.snipmatch.Snippet;
import org.eclipse.recommenders.snipmatch.rcp.SnippetEditor;
import org.eclipse.recommenders.snipmatch.rcp.SnippetEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

@SuppressWarnings("restriction")
public class CreateSnippetHandler extends AbstractHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CreateSnippetHandler.class);

    private Set<ISnippetRepository> repos;

    private ExecutionEvent event;

    @Inject
    public CreateSnippetHandler(Set<ISnippetRepository> repos) {
        this.repos = repos;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        this.event = event;
        CompilationUnitEditor editor = cast(HandlerUtil.getActiveEditor(event));
        Snippet snippet = createSnippet(editor);
        openSnippetInEditor(snippet);
        return null;
    }

    @VisibleForTesting
    public Snippet createSnippet(CompilationUnitEditor editor) throws ExecutionException {
        ISourceViewer viewer = editor.getViewer();
        ITypeRoot root = cast(editor.getViewPartInput());
        CompilationUnit ast = SharedASTProvider.getAST(root, SharedASTProvider.WAIT_YES, null);

        IDocument doc = viewer.getDocument();
        ITextSelection textSelection = cast(viewer.getSelectionProvider().getSelection());

        return new SnippetBuilder(ast, doc, textSelection).build("<new snippet>", "<enter description>"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void openSnippetInEditor(Snippet snippet) {
        for (ISnippetRepository r : repos) {
            if (r.isImportSupported()) {
                try {
                    SnippetEditorInput input = new SnippetEditorInput(snippet, r);
                    IWorkbenchPage page = getActiveWorkbenchWindow(event).getActivePage();
                    SnippetEditor ed = cast(page.openEditor(input, EDITOR_ID));
                    // mark the editor dirty when opening a newly created snippet
                    ed.markDirtyUponSnippetCreation();
                    // if we could add the snippet somewhere, return. Otherwise report an error
                    return;
                } catch (PartInitException e) {
                    LOG.error(Messages.ERROR_WHILE_OPENING_EDITOR, e);
                }
            }
        }
        openError(HandlerUtil.getActiveShell(event), Messages.ERROR_NO_EDITABLE_REPO_FOUND,
                Messages.ERROR_NO_EDITABLE_REPO_FOUND_HINT);
    }

}
