package org.eclipse.recommenders.internal.completion.rcp.proposals;

import static com.google.common.base.Optional.fromNullable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.internal.ui.text.java.MethodDeclarationCompletionProposal;
import org.eclipse.recommenders.completion.rcp.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.ProposalProcessorManager;

import com.google.common.base.Optional;

public class ProcessableMethodDeclarationCompletionProposal extends MethodDeclarationCompletionProposal implements
        IProcessableProposal {

    public static ProcessableMethodDeclarationCompletionProposal newProposal(CompletionProposal proposal, IType type,
            int relevance) throws CoreException {

        String prefix = String.valueOf(proposal.getName());
        int offset = proposal.getReplaceStart();
        int length = proposal.getReplaceEnd() - offset;

        IMethod[] methods = type.getMethods();
        if (!type.isInterface()) {
            String constructorName = type.getElementName();
            if (constructorName.length() > 0 && constructorName.startsWith(prefix)
                    && !hasMethod(methods, constructorName)) {
                return new ProcessableMethodDeclarationCompletionProposal(type, constructorName, null, offset, length,
                        relevance + 500);
            }
        }

        if (prefix.length() > 0 && !"main".equals(prefix) && !hasMethod(methods, prefix)) {
            if (!JavaConventionsUtil.validateMethodName(prefix, type).matches(IStatus.ERROR))
                return new ProcessableMethodDeclarationCompletionProposal(type, prefix, Signature.SIG_VOID, offset,
                        length, relevance);
        }
        return null;
    }

    private static boolean hasMethod(IMethod[] methods, String name) {
        for (int i = 0; i < methods.length; i++) {
            IMethod curr = methods[i];
            if (curr.getElementName().equals(name) && curr.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }

    private CompletionProposal coreProposal;
    private ProposalProcessorManager mgr;
    private String lastPrefix;

    public ProcessableMethodDeclarationCompletionProposal(IType type, String methodName, String returnTypeSig,
            int start, int length, int relevance) {
        super(type, methodName, returnTypeSig, start, length, relevance);
    }

    // ===========

    @Override
    public boolean isPrefix(final String prefix, final String completion) {
        lastPrefix = prefix;
        if (mgr.prefixChanged(prefix)) {
            return true;
        }
        return super.isPrefix(prefix, completion);
    }

    @Override
    public String getPrefix() {
        return lastPrefix;
    }

    @Override
    public Optional<CompletionProposal> getCoreProposal() {
        return fromNullable(coreProposal);
    }

    @Override
    public ProposalProcessorManager getProposalProcessorManager() {
        return mgr;
    }

    @Override
    public void setProposalProcessorManager(ProposalProcessorManager mgr) {
        this.mgr = mgr;
    }

}