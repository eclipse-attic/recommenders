package org.eclipse.recommenders.internal.completion.rcp.subwords2;

import static org.eclipse.jdt.core.Signature.getReturnType;
import static org.eclipse.recommenders.internal.completion.rcp.ProcessableCompletionProposalComputer.NULL_PROPOSAL;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Assignment;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.recommenders.completion.rcp.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.completion.rcp.ProposalProcessor;
import org.eclipse.recommenders.completion.rcp.SessionProcessor;

public class Subwords2SessionProcessor extends SessionProcessor {

    private boolean skip = false;
    private String varName;

    @Override
    public void startSession(IRecommendersCompletionContext ctx) {
        ASTNode node = ctx.getCompletionNodeParent().orNull();
        if (node instanceof Assignment) {
            varName = findVarName((Assignment) node);
        } else if (node instanceof LocalDeclaration) {
            varName = findVarName((LocalDeclaration) node);
        }
        skip = StringUtils.length(varName) < 3;
    }

    private String findVarName(final LocalDeclaration node) {
        return toLowerCaseString(node.name);
    }

    String toLowerCaseString(final char[] name) {
        if (name == null) {
            return null;
        }
        return String.valueOf(name).toLowerCase();
    }

    private String findVarName(final Assignment node) {
        if (!(node.lhs instanceof SingleNameReference)) {
            return null;
        }
        SingleNameReference lhs = (SingleNameReference) node.lhs;
        return toLowerCaseString(lhs.token);
    }

    @Override
    public void process(final IProcessableProposal proposal) {
        if (skip) return;

        CompletionProposal core = proposal.getCoreProposal().or(NULL_PROPOSAL);
        switch (core.getKind()) {
        case CompletionProposal.METHOD_REF:
            String name = String.valueOf(core.getName());
            if (StringUtils.containsIgnoreCase(name, varName) && !isVoid(core)) {
                proposal.getProposalProcessorManager().addProcessor(new ProposalProcessor() {
                    @Override
                    public void modifyRelevance(AtomicInteger relevance) {
                        relevance.addAndGet(5);
                    }

                    public void modifyDisplayString(StyledString displayString) {
                        displayString.append(" .oO", StyledString.COUNTER_STYLER);
                    };
                });
            }
        }
    }

    private boolean isVoid(CompletionProposal core) {
        return ArrayUtils.isEquals(getReturnType(core.getSignature()), new char[] { 'V' });
    }
}
