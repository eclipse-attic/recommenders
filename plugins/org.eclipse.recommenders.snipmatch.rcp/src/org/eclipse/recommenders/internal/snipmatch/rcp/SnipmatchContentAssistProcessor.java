/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch, Madhuranga Lakjeewa - initial API and implementation.
 */
package org.eclipse.recommenders.internal.snipmatch.rcp;

import static org.eclipse.recommenders.internal.snipmatch.rcp.Constants.SNIPMATCH_CONTEXT_ID;
import static org.eclipse.recommenders.utils.Logs.log;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.recommenders.rcp.SharedImages;
import org.eclipse.recommenders.snipmatch.ISnippet;
import org.eclipse.recommenders.snipmatch.ISnippetRepository;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@SuppressWarnings("restriction")
public class SnipmatchContentAssistProcessor implements IContentAssistProcessor {

<<<<<<< HEAD   (6fbe47 [releng] Remove stacktraces.model .project from version cont)
    private final Set<ISnippetRepository> repos;
    private final TemplateContextType snipmatchContextType;
=======
    private final Repositories repos;

    private final TemplateContextType contextType;
>>>>>>> BRANCH (c268cb [snipmatch] SWTBot Tests for Snippets View)
    private final Image image;

    private JavaContentAssistInvocationContext ctx;
    private String terms;

    @Inject
    public SnipmatchContentAssistProcessor(Repositories repos, SharedImages images) {
        this.repos = repos;
        snipmatchContextType = SnipmatchTemplateContextType.getInstance();
        image = images.getImage(SharedImages.Images.OBJ_BULLET_BLUE);
    }

    public void setContext(JavaContentAssistInvocationContext ctx) {
        this.ctx = ctx;
    }

    public void setTerms(String query) {
        terms = query;
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

        if (StringUtils.isEmpty(terms)) {
            return new ICompletionProposal[0];
        }

        JavaEditorSearchContext context = new JavaEditorSearchContext(terms, ctx);

        LinkedList<ICompletionProposal> proposals = Lists.newLinkedList();
        List<Recommendation<ISnippet>> recommendations = Lists.newArrayList();
<<<<<<< HEAD   (6fbe47 [releng] Remove stacktraces.model .project from version cont)
        for (ISnippetRepository repo : repos) {
            recommendations.addAll(repo.search(context));
=======
        for (ISnippetRepository repo : repos.getRepositories()) {
            recommendations.addAll(repo.search(terms));
>>>>>>> BRANCH (c268cb [snipmatch] SWTBot Tests for Snippets View)
        }
        ICompilationUnit cu = ctx.getCompilationUnit();
        IEditorPart editor = EditorUtility.isOpenInEditor(cu);

        ISourceViewer sourceViewer = (ISourceViewer) editor.getAdapter(ITextOperationTarget.class);
        Point selection = sourceViewer.getSelectedRange();
        IRegion region = new Region(selection.x, selection.y);
        Position p = new Position(selection.x, selection.y);
        IDocument document = sourceViewer.getDocument();

        String selectedText = null;
        if (selection.y != 0) {
            try {
                selectedText = document.get(selection.x, selection.y);
            } catch (BadLocationException e) {
            }
        }

        JavaContext ctx = new JavaContext(snipmatchContextType, document, p, cu);
        ctx.setVariable("selection", selectedText); //$NON-NLS-1$
        ctx.setForceEvaluation(true);

        for (Recommendation<ISnippet> recommendation : recommendations) {
            ISnippet snippet = recommendation.getProposal();
            Template template = new Template(snippet.getName(), snippet.getDescription(), SNIPMATCH_CONTEXT_ID,
                    snippet.getCode(), true);

            try {
                proposals.add(SnippetProposal.newSnippetProposal(recommendation, template, ctx, region, image));
            } catch (Exception e) {
                log(LogMessages.ERROR_CREATING_SNIPPET_PROPOSAL_FAILED, e);
            }
        }
        return Iterables.toArray(proposals, ICompletionProposal.class);
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return null;
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }
}
