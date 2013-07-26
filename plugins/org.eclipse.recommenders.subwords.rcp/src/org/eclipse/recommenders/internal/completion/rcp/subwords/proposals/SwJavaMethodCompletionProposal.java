/**
 * Copyright (c) 2010 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.completion.rcp.subwords.proposals;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.JavaMethodCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.recommenders.internal.completion.rcp.subwords.SubwordsProposalContext;

@SuppressWarnings("restriction")
public final class SwJavaMethodCompletionProposal extends JavaMethodCompletionProposal {

    private final SubwordsProposalContext subwordsContext;

    protected SwJavaMethodCompletionProposal(final CompletionProposal proposal,
            final JavaContentAssistInvocationContext context, final SubwordsProposalContext subwordsContext) {
        super(proposal, context);
        this.subwordsContext = subwordsContext;
        setRelevance(subwordsContext.calculateRelevance());
    }

    @Override
    public boolean isPrefix(final String prefix, final String completion) {
        subwordsContext.setPrefix(prefix);
        setRelevance(subwordsContext.calculateRelevance());
        return subwordsContext.isRegexMatch();
    }

    @Override
    public StyledString getStyledDisplayString() {
        final StyledString origin = super.getStyledDisplayString();
        return subwordsContext.getStyledDisplayString(origin);
    }
}
