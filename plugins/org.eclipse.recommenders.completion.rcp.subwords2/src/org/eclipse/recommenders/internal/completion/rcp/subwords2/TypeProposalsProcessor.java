package org.eclipse.recommenders.internal.completion.rcp.subwords2;

import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.eclipse.recommenders.internal.completion.rcp.ProcessableCompletionProposalComputer.NULL_PROPOSAL;
import static org.eclipse.recommenders.utils.names.VmPackageName.DEFAULT_PACKAGE;
import static org.eclipse.recommenders.utils.rcp.ast.BindingUtils.toPackageName;
import static org.eclipse.recommenders.utils.rcp.ast.BindingUtils.toPackageNames;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnFieldType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMethodReturnType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.recommenders.completion.rcp.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.completion.rcp.ProposalProcessor;
import org.eclipse.recommenders.completion.rcp.SessionProcessor;
import org.eclipse.recommenders.utils.names.IPackageName;
import org.eclipse.recommenders.utils.names.ITypeName;
import org.eclipse.recommenders.utils.names.VmPackageName;
import org.eclipse.recommenders.utils.names.VmTypeName;
import org.eclipse.recommenders.utils.rcp.JavaElementResolver;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public final class TypeProposalsProcessor extends SessionProcessor {

    private final class TypesProposalProcessor extends ProposalProcessor {
        @Override
        public void modifyRelevance(AtomicInteger relevance) {
            relevance.addAndGet(4);
        }

        @Override
        public void modifyDisplayString(StyledString displayString) {
            displayString.append(" - pkg", StyledString.COUNTER_STYLER);
        }
    }

    static final String TRAILS = "trails";
    static final Set<Class<?>> SUPPORTED_COMPLETION_NODES = new HashSet<Class<?>>() {
        {
            add(CompletionOnMethodReturnType.class);
            add(CompletionOnSingleNameReference.class);
            add(CompletionOnFieldType.class);
            // List<String> l = new |^space
            add(CompletionOnSingleTypeReference.class);
        }
    };

    private Set<IPackageName> pkgs;
    private IType expectedType;
    private JavaElementResolver jdtCache;
    private HashSet<String> expectedSubwords;

    @Inject
    public TypeProposalsProcessor(JavaElementResolver jdtCache) {
        this.jdtCache = jdtCache;
    }

    @Override
    public void startSession(IRecommendersCompletionContext context) {
        expectedType = context.getExpectedType().orNull();

        expectedSubwords = Sets.newHashSet();
        if (expectedType != null) {
            String[] split1 = expectedType.getElementName().split("(?=\\p{Upper})");
            {
                for (String s : split1) {
                    if (s.length() > 3) {
                        expectedSubwords.add(s);
                    }
                }
            }
        }

        pkgs = Sets.newHashSet();

        ASTNode completion = context.getCompletionNode().orNull();
        if (completion == null || !SUPPORTED_COMPLETION_NODES.contains(completion.getClass())) {
            return;
        }

        CompilationUnit ast = context.getAST();
        ast.accept(new ASTVisitor() {

            @Override
            public boolean visit(PackageDeclaration node) {
                pkgs.add(toPackageName(node.resolveBinding()).or(DEFAULT_PACKAGE));
                return false;
            }

            @Override
            public boolean visit(SimpleType node) {
                pkgs.add(toPackageName(node.resolveBinding()).or(DEFAULT_PACKAGE));
                return super.visit(node);
            }

            @Override
            public boolean visit(SuperMethodInvocation node) {
                return visit(node.resolveMethodBinding());
            }

            @Override
            public boolean visit(MethodInvocation node) {
                return visit(node.resolveMethodBinding());
            }

            private boolean visit(IMethodBinding b) {
                if (b == null) return true;
                pkgs.add(toPackageName(b.getReturnType()).or(DEFAULT_PACKAGE));
                pkgs.addAll(toPackageNames(b.getParameterTypes()));
                return true;
            }
        });
        // remove omnipresent package
        pkgs.remove(VmPackageName.get("java/lang"));
    }

    @Override
    public void process(IProcessableProposal proposal) throws JavaModelException {

        final CompletionProposal coreProposal = proposal.getCoreProposal().or(NULL_PROPOSAL);
        switch (coreProposal.getKind()) {
        case CompletionProposal.CONSTRUCTOR_INVOCATION:
            handleConstructorProposal(proposal, coreProposal);
            break;
        case CompletionProposal.TYPE_REF:
        case CompletionProposal.TYPE_IMPORT:
            handleTypeProposal(proposal, coreProposal);
        }
    }

    private void handleTypeProposal(IProcessableProposal proposal, final CompletionProposal coreProposal) {
        String sig = new String(coreProposal.getSignature());
        sig = sig.replace('.', '/');
        sig = StringUtils.removeEnd(sig, ";");
        ITypeName type = VmTypeName.get(sig);
        IPackageName pkg = type.getPackage();
        if (pkgs.contains(pkg)) {
            proposal.getProposalProcessorManager().addProcessor(new TypesProposalProcessor());
        }
    }

    private void handleConstructorProposal(IProcessableProposal proposal, final CompletionProposal coreProposal) {
        String name = removeEnd(valueOf(coreProposal.getDeclarationSignature()).replace('.', '/'), ";");
        VmTypeName recType = VmTypeName.get(name);
        if (pkgs.contains(recType.getPackage())) {
            proposal.getProposalProcessorManager().addProcessor(new TypesProposalProcessor());
        }
        if (expectedType != null) {
            Set<String> s2 = Sets.newHashSet(recType.getClassName().split("(?=\\p{Upper})"));
            final SetView<String> intersection = Sets.intersection(s2, expectedSubwords);
            if (!intersection.isEmpty())
                proposal.getProposalProcessorManager().addProcessor(new ProposalProcessor() {
                    @Override
                    public void modifyRelevance(AtomicInteger relevance) {
                        relevance.addAndGet(intersection.size());
                    }

                    @Override
                    public void modifyDisplayString(StyledString displayString) {
                        displayString.append(" - partial", StyledString.COUNTER_STYLER);
                    }
                });
        }
    }
}