/**
 * Copyright (c) 2010, 2014 Darmstadt University of Technology. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Andreas Sewe - initial API and implementation.
 */
package org.eclipse.recommenders.completion.rcp.tips;

import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.internal.completion.rcp.l10n.Messages;
import org.eclipse.swt.widgets.Shell;

@SuppressWarnings("restriction")
public abstract class AbstractCompletionTipProposal extends AbstractJavaCompletionProposal
        implements ICompletionTipProposal {

    private static final Object DUMMY_INFO = new Object();

    /**
     * Don't sort this proposal based on its relevance or label, but always show it below all other proposals.
     *
     * We have to use -10001 as Integer.MIN_VALUE does not work (possibly due to underflow) and other proposals (e.g.,
     * Subwords matches) can have a relevance of -10000.
     */
    private static final int RELEVANCE = -10001;
    private static final String SORT_STRING = "\uFFFF";

    private long suppressProposalDeadlineMillis = 0;

    public AbstractCompletionTipProposal() {
        // setReplacementOffset called during setInvocationOffset below
        setReplacementString(""); //$NON-NLS-1$
        setReplacementLength(0);

        setRelevance(RELEVANCE);

        setSortString(SORT_STRING);

        setCursorPosition(0);
    }

    @Override
    public void setInvocationOffset(int invocationOffset) {
        setReplacementOffset(invocationOffset);
    }

    @Override
    public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
        return DUMMY_INFO;
    }

    @Override
    protected boolean isValidPrefix(String prefix) {
        return true;
    }

    @Override
    public boolean isApplicable(IRecommendersCompletionContext context) {
        return !isProposalSuppressed();
    }

    @Override
    public IInformationControlCreator getInformationControlCreator() {
        return new IInformationControlCreator() {

            @Override
            public IInformationControl createInformationControl(Shell parent) {
                return AbstractCompletionTipProposal.this.createInformationControl(parent,
                        Messages.PROPOSAL_CATEGORY_CODE_RECOMMENDERS);
            }
        };
    }

    protected abstract IInformationControl createInformationControl(Shell parent, String statusLineText);

    protected void suppressProposal(long suppressionTimespan, TimeUnit timeUnit) {
        suppressProposalDeadlineMillis = System.currentTimeMillis() + timeUnit.toMillis(suppressionTimespan);
    }

    protected boolean isProposalSuppressed() {
        return System.currentTimeMillis() <= suppressProposalDeadlineMillis;
    }
}
