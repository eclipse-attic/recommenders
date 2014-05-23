/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andreas Sewe - initial API and implementation.
 */
package org.eclipse.recommenders.internal.snipmatch.rcp;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;

import java.text.MessageFormat;

import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.ui.javaeditor.IndentUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.recommenders.snipmatch.ISnippet;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.swt.graphics.Image;

public class SnippetProposal extends TemplateProposal {

    private final ISnippet snippet;

    private final boolean valid;

    public static SnippetProposal newSnippetProposal(Recommendation<ISnippet> recommendation, Template template,
            TemplateContext context, IRegion region, Image image) throws BadLocationException, TemplateException {
        boolean valid = false;
        try {
            context.evaluate(template);
            valid = true;
        } catch (Exception e) {
            context = new JavaContext(context.getContextType(), new Document(), new Position(0), null);
            context.evaluate(template);
        }
        int relevance = (int) (recommendation.getRelevance() * 100);
        return new SnippetProposal(recommendation.getProposal(), relevance, template, context, region, image, valid);
    }

    private SnippetProposal(ISnippet snippet, int relevance, Template template, TemplateContext context,
            IRegion region, Image image, boolean valid) {
        super(template, context, region, image, relevance);
        this.snippet = snippet;
        this.valid = valid;
    }

    @Override
    public boolean isValidFor(IDocument document, int offset) {
        return valid;
    }

    @Override
    public String getAdditionalProposalInfo() {
        StringBuilder header = new StringBuilder();

        if (!valid) {
            header.append(MessageFormat.format(Messages.WARNING_CANNOT_APPLY_SNIPPET, "// XXX")); // NON-NLS-1$
            header.append(LINE_SEPARATOR);
            header.append(MessageFormat.format(Messages.WARNING_REPOSITION_CURSOR, "// FIXME")); // NON-NLS-1$
            header.append(LINE_SEPARATOR);
            header.append(LINE_SEPARATOR);
        }

        if (!isEmpty(snippet.getDescription())) {
            header.append("// "); //$NON-NLS-1$
            header.append(snippet.getDescription());
            header.append(LINE_SEPARATOR);
        }

        try {
            return fixIndentation(header + super.getAdditionalProposalInfo());
        } catch (BadLocationException e) {
            return null;
        }
    }

    private String fixIndentation(String additionalProposalInfo) throws BadLocationException {
        IDocument document = new Document(additionalProposalInfo);
        IndentUtil.indentLines(document, new LineRange(0, document.getNumberOfLines()), null, null);
        return document.get();
    }

    @Override
    public String getDisplayString() {
        return createDisplayString(snippet);
    }

    public static String createDisplayString(ISnippet snippet) {
        if (isNullOrEmpty(snippet.getDescription())) {
            return snippet.getName();
        } else {
            return MessageFormat.format("{0} \u2013 {1}", snippet.getName(), snippet.getDescription()); // NON-NLS-1$
        }
    }

    public ISnippet getSnippet() {
        return snippet;
    }
}
