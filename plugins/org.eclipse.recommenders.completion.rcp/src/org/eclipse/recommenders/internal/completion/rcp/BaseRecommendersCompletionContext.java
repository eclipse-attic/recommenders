/**
 * Copyright (c) 2010, 2011 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.completion.rcp;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static org.eclipse.recommenders.utils.Checks.cast;

import java.util.Map;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.codeassist.InternalCompletionContext;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnLocalName;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMemberAccess;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.ThisReference;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MissingTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.Region;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.rcp.IAstProvider;
import org.eclipse.recommenders.rcp.RecommendersPlugin;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.rcp.CompilerBindings;
import org.eclipse.recommenders.utils.rcp.JdtUtils;

import com.google.common.base.Optional;

@SuppressWarnings("restriction")
public abstract class BaseRecommendersCompletionContext implements IRecommendersCompletionContext {

    private final JavaContentAssistInvocationContext javaContext;
    private InternalCompletionContext coreContext;
    private final IAstProvider astProvider;
    private ProposalCollectingCompletionRequestor collector;

    public BaseRecommendersCompletionContext(final JavaContentAssistInvocationContext jdtContext,
            final IAstProvider astProvider) {
        this.javaContext = jdtContext;
        this.astProvider = astProvider;
        this.coreContext = cast(jdtContext.getCoreContext());
        // if (!coreContext.isExtended()) {
        requestExtendedContext();
        // }
    }

    private void requestExtendedContext() {
        final ICompilationUnit cu = getCompilationUnit();

        collector = new ProposalCollectingCompletionRequestor(javaContext);
        try {
            cu.codeComplete(getInvocationOffset(), collector);
        } catch (final JavaModelException e) {
            RecommendersPlugin.log(e);
        }
        coreContext = collector.getCoreContext();
    }

    public InternalCompletionContext getCoreContext() {
        return coreContext;
    }

    @Override
    public JavaContentAssistInvocationContext getJavaContext() {
        return javaContext;
    }

    @Override
    public IJavaProject getProject() {
        return javaContext.getProject();
    };

    @Override
    public int getInvocationOffset() {
        return javaContext.getInvocationOffset();
    }

    @Override
    public Region getReplacementRange() {
        final int offset = getInvocationOffset();
        final int length = getPrefix().length();
        return new Region(offset, length);
    }

    @Override
    public Optional<IMethod> getEnclosingMethod() {
        final IJavaElement enclosing = getEnclosingElement().orNull();
        if (enclosing instanceof IMethod) {
            return of((IMethod) enclosing);
        } else {
            return absent();
        }
    }

    @Override
    public Optional<IType> getEnclosingType() {
        final IJavaElement enclosing = getEnclosingElement().orNull();
        if (enclosing instanceof IType) {
            return of((IType) enclosing);
        } else {
            return absent();
        }
    }

    @Override
    public Optional<IJavaElement> getEnclosingElement() {
        if (coreContext.isExtended()) {
            return fromNullable(coreContext.getEnclosingElement());
        }
        return absent();
    }

    @Override
    public boolean hasEnclosingElement() {
        return getEnclosingElement().isPresent();
    }

    @Override
    public Optional<IType> getClosestEnclosingType() {
        if (!hasEnclosingElement()) {
            absent();
        }
        final IJavaElement enclosing = getEnclosingElement().get();
        if (enclosing instanceof IType) {
            return of((IType) enclosing);
        } else {
            final IType type = (IType) enclosing.getAncestor(IJavaElement.TYPE);
            return fromNullable(type);
        }
    }

    @Override
    public boolean isCompletionInMethodBody() {
        return getEnclosingMethod().isPresent();
    }

    @Override
    public boolean isCompletionInTypeBody() {
        return getEnclosingType().isPresent();
    }

    @Override
    public ICompilationUnit getCompilationUnit() {
        return javaContext.getCompilationUnit();
    }

    @Override
    public CompilationUnit getAST() {
        return astProvider.get(getCompilationUnit());
    }

    @Override
    public Map<IJavaCompletionProposal, CompletionProposal> getProposals() {
        return collector.getProposals();
    }

    @Override
    public Optional<String> getExpectedTypeSignature() {
        final char[][] keys = coreContext.getExpectedTypesKeys();
        if (keys == null) {
            return absent();
        }
        if (keys.length < 1) {
            return absent();
        }
        final String res = new String(keys[0]);
        return of(res);
    }

    @Override
    public Optional<IType> getExpectedType() {
        final IType res = javaContext.getExpectedType();
        return fromNullable(res);
    }

    @Override
    public String getPrefix() {
        final char[] token = coreContext.getToken();
        if (token == null) {
            return "";
        }
        return new String(token);
    }

    @Override
    public String getReceiverName() {

        final ASTNode n = getCompletionNode();
        char[] name = null;
        if (n instanceof CompletionOnQualifiedNameReference) {
            final CompletionOnQualifiedNameReference c = cast(n);
            switch (c.binding.kind()) {
            case Binding.VARIABLE:
            case Binding.FIELD:
            case Binding.LOCAL:
                final VariableBinding b = (VariableBinding) c.binding;
                name = b.name;
                break;
            }
        } else if (n instanceof CompletionOnLocalName) {
            final CompletionOnLocalName c = cast(n);
            name = c.realName;
        } else if (n instanceof CompletionOnSingleNameReference) {
            final CompletionOnSingleNameReference c = cast(n);
            name = c.token;
        } else if (n instanceof CompletionOnMemberAccess) {
            final CompletionOnMemberAccess c = cast(n);
            if (c.receiver instanceof ThisReference) {
                name = "this".toCharArray();
            } else if (c.receiver instanceof MessageSend) {
                // some anonymous type/method return value that has no name...
                // e.g.:
                // PlatformUI.getWorkbench()|^Space --> receiver is anonymous
                // --> name = null
                name = null;
            } else if (c.fieldBinding() != null) {
                // does this happen? When?
                name = c.fieldBinding().name;
            } else if (c.localVariableBinding() != null) {
                // does this happen? when?
                name = c.localVariableBinding().name;
            }
        }
        return toString(name);
    }

    private String toString(final char[] name) {
        if (name == null) {
            return "";
        }
        // remove all whitespaces:
        return new String(name).replace(" ", "");
    }

    @Override
    public Optional<String> getReceiverTypeSignature() {
        final Optional<TypeBinding> opt = findReceiverTypeBinding();
        return toString(opt.orNull());
    }

    private Optional<TypeBinding> findReceiverTypeBinding() {
        final ASTNode n = getCompletionNode();
        TypeBinding receiver = null;
        if (n instanceof CompletionOnLocalName) {
            // final CompletionOnLocalName c = cast(n);
            // name = c.realName;
        } else if (n instanceof CompletionOnSingleNameReference) {
            final CompletionOnSingleNameReference c = cast(n);
            receiver = c.resolvedType;
        } else if (n instanceof CompletionOnQualifiedNameReference) {
            final CompletionOnQualifiedNameReference c = cast(n);
            switch (c.binding.kind()) {
            case Binding.VARIABLE:
            case Binding.FIELD:
            case Binding.LOCAL:
                final VariableBinding varBinding = (VariableBinding) c.binding;
                receiver = varBinding.type;
                break;
            case Binding.TYPE:
                // e.g. PlatformUI.|<ctrl-space>
                receiver = (TypeBinding) c.binding;
                break;
            default:
            }
        } else if (n instanceof CompletionOnMemberAccess) {
            final CompletionOnMemberAccess c = cast(n);
            receiver = c.actualReceiverType;
        }
        return fromNullable(receiver);
    }

    private Optional<String> toString(final TypeBinding receiver) {
        if (receiver == null) {
            return absent();
        }
        final String res = new String(receiver.signature());
        return of(res);
    }

    @Override
    public Optional<IType> getReceiverType() {
        final Optional<TypeBinding> opt = findReceiverTypeBinding();
        if (!opt.isPresent()) {
            return absent();
        }
        final TypeBinding b = opt.get();
        if (b instanceof MissingTypeBinding) {
            return absent();
        }
        return JdtUtils.createUnresolvedType(b);
    }

    @Override
    public Optional<IMethodName> getMethodDef() {
        final ASTNode node = getCompletionNode();
        if (node instanceof CompletionOnMemberAccess) {
            final CompletionOnMemberAccess n = cast(node);
            if (n.receiver instanceof MessageSend) {
                final MessageSend receiver = (MessageSend) n.receiver;
                final MethodBinding binding = receiver.binding;
                return CompilerBindings.toMethodName(binding);
            }
        }
        return absent();
    }
}
