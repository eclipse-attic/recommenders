/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.sandbox.rcp;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.completion.rcp.processable.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.processable.ProposalProcessorManager;
import org.eclipse.recommenders.completion.rcp.processable.SessionProcessor;
import org.eclipse.recommenders.completion.rcp.processable.SimpleProposalProcessor;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class HippieCompletionSessionProcessor extends SessionProcessor {

    Multiset<String> keys = HashMultiset.create();

    @Override
    public boolean startSession(IRecommendersCompletionContext ctx) {
        if (keys.size() > 50) {
            // TODO surprise!
            // well, we should use some LRU cache. #prototype impl
            keys.clear();
        }
        return true;
    }

    @Override
    public void process(IProcessableProposal proposal) throws Exception {
        CompletionProposal c = proposal.getCoreProposal().orNull();
        if (c == null) {
            return;
        }
        final int count = keys.count(key(c));
        if (count > 0) {
            String label = String.format(Messages.HIPPIE_COMPLETION_PROPOSAL, count);
            ProposalProcessorManager mgr = proposal.getProposalProcessorManager();
            int increment = Math.min(10 << 2, count);
            mgr.addProcessor(new SimpleProposalProcessor(increment, label));
        }
    }

    @Override
    public void applied(ICompletionProposal proposal) {
        if (!(proposal instanceof IProcessableProposal)) {
            return;
        }

        IProcessableProposal p = (IProcessableProposal) proposal;
        CompletionProposal c = p.getCoreProposal().orNull();
        if (c == null) {
            return;
        }

        // switch (c.getKind()) {
        // case CompletionProposal.CONSTRUCTOR_INVOCATION:
        // case CompletionProposal.TYPE_REF:
        // case CompletionProposal.METHOD_DECLARATION:
        // case CompletionProposal.METHOD_REF:
        // case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
        // case CompletionProposal.FIELD_REF:
        // case CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER:
        // case CompletionProposal.LOCAL_VARIABLE_REF:
        String key = key(c);
        if (key.length() > 3) {
            keys.add(key);
        }
        // default:
        // }
    }

    private String key(CompletionProposal c) {
        StringBuilder sb = new StringBuilder();
        char[] name = c.getName();
        char[] signature = c.getSignature();
        char[] declarationSignature = c.getDeclarationSignature();
        if (name != null) {
            sb.append(name);
        }
        if (signature != null) {
            sb.append(signature);
        }
        if (declarationSignature != null) {
            sb.append(declarationSignature);
        }
        return sb.toString();
    }

}
