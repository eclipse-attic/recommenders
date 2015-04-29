/**
 * Copyright (c) 2010, 2014 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 *    Andreas Sewe - better handling of generics.
 *    Johannes Dorn - refactoring.
 */
package org.eclipse.recommenders.completion.rcp.utils;

import static com.google.common.base.Optional.absent;
import static java.lang.Math.min;
import static org.eclipse.jdt.core.compiler.CharOperation.splitOn;
import static org.eclipse.recommenders.internal.completion.rcp.LogMessages.ERROR_COMPILATION_FAILURE_PREVENTS_PROPOSAL_MATCHING;
import static org.eclipse.recommenders.utils.Checks.cast;
import static org.eclipse.recommenders.utils.Logs.log;
import static org.eclipse.recommenders.utils.Reflections.getDeclaredField;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.codeassist.CompletionEngine;
import org.eclipse.jdt.internal.codeassist.InternalCompletionProposal;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.recommenders.rcp.utils.CompilerBindings;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.VmMethodName;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

@SuppressWarnings("restriction")
public final class ProposalUtils {

    private ProposalUtils() {
        // Not meant to be instantiated
    }

    private static final IMethodName OBJECT_CLONE = VmMethodName.get("Ljava/lang/Object.clone()Ljava/lang/Object;");

    private static final char[] INIT = "<init>".toCharArray(); //$NON-NLS-1$

    /**
     * Workaround needed to handle proposals with generic signatures properly.
     *
     * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=380203"Bug 380203</a>.
     */
    private static final Field ORIGINAL_SIGNATURE = getDeclaredField(InternalCompletionProposal.class, "originalSignature") //$NON-NLS-1$
            .orNull();

    private static char[] getSignature(CompletionProposal proposal) {
        char[] signature = null;
        try {
            if (canUseReflection(proposal)) {
                signature = (char[]) ORIGINAL_SIGNATURE.get(proposal);
            }
        } catch (Exception e) {
            log(org.eclipse.recommenders.utils.LogMessages.LOG_WARNING_REFLECTION_FAILED, e, ORIGINAL_SIGNATURE);
        }
        return signature != null ? signature : proposal.getSignature();
    }

    private static boolean canUseReflection(CompletionProposal proposal) {
        return proposal instanceof InternalCompletionProposal && ORIGINAL_SIGNATURE != null
                && ORIGINAL_SIGNATURE.isAccessible();
    }

    /**
     * @see <a href="https://www.eclipse.org/forums/index.php/m/1408138/">Forum discussion of the lookup strategy
     *      employed by this method</a>
     */
    public static Optional<IMethodName> toMethodName(CompletionProposal proposal, LookupEnvironment env) {
        Preconditions.checkArgument(isKindSupported(proposal));

        if (isArrayCloneMethod(proposal)) {
            char[] signature = proposal.getSignature();
            char[] receiverSignature = proposal.getReceiverSignature();
            return Optional.of(OBJECT_CLONE);
        }

        ReferenceBinding declaringType = getDeclaringType(proposal, env).orNull();
        if (declaringType == null) {
            return absent();
        }

        char[] methodName = proposal.isConstructor() ? INIT : proposal.getName();
        MethodBinding[] overloads;
        try {
            overloads = declaringType.getMethods(methodName);
        } catch (AbortCompilation e) {
            log(ERROR_COMPILATION_FAILURE_PREVENTS_PROPOSAL_MATCHING, null, proposal);
            return absent();
        }

        char[] proposalSignature = getSignature(proposal);
        char[] strippedProposalSignature = stripTypeParameters(proposalSignature);

        for (MethodBinding overload : overloads) {
            char[] signature = CompletionEngine.getSignature(overload);

            if (Arrays.equals(proposalSignature, signature)) {
                return CompilerBindings.toMethodName(overload);
            }
            if (Arrays.equals(strippedProposalSignature, signature)) {
                return CompilerBindings.toMethodName(overload);
            }
        }

        return absent();
    }

    private static boolean isKindSupported(CompletionProposal proposal) {
        switch (proposal.getKind()) {
        case CompletionProposal.METHOD_REF:
            return true;
        case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
            return true;
        case CompletionProposal.METHOD_DECLARATION:
            return true;
        case CompletionProposal.CONSTRUCTOR_INVOCATION:
            return true;
        default:
            return false;
        }
    }

    private static boolean isArrayCloneMethod(CompletionProposal proposal) {
        if (proposal.isConstructor()) {
            // Not a method proposal
            return false;
        }

        char[] declarationSignature = proposal.getDeclarationSignature();
        if (declarationSignature[0] != '[') {
            // Not an array
            return false;
        }

        if (!CharOperation.equals(TypeConstants.CLONE, proposal.getName())) {
            // Not named clone
            return false;
        }

        char[] signature = proposal.getSignature();
        if (signature.length != declarationSignature.length + 2 || signature[0] != '(' || signature[1] != ')') {
            // Overload of real (no-args) clone method
            return false;
        }

        if (!CharOperation.endsWith(signature, declarationSignature)) {
            // Wrong return type
            return false;
        }

        return true;
    }

    private static char[] stripTypeParameters(char[] proposalSignature) {
        StringBuilder sb = new StringBuilder();

        // Copy optional type parameters
        sb.append(proposalSignature, 0, ArrayUtils.indexOf(proposalSignature, Signature.C_PARAM_START));

        sb.append(Signature.C_PARAM_START);
        char[][] parameterTypes = Signature.getParameterTypes(proposalSignature);
        for (char[] parameterType : parameterTypes) {
            sb.append(Signature.getTypeErasure(parameterType));
        }
        sb.append(Signature.C_PARAM_END);

        char[] returnType = Signature.getReturnType(proposalSignature);
        sb.append(Signature.getTypeErasure(returnType));

        char[][] exceptionTypes = Signature.getThrownExceptionTypes(proposalSignature);
        if (exceptionTypes.length > 0) {
            sb.append(Signature.C_EXCEPTION_START);
            for (char[] exceptionType : exceptionTypes) {
                sb.append(exceptionType);
            }
        }

        return sb.toString().toCharArray();
    }

    private static Optional<ReferenceBinding> getDeclaringType(CompletionProposal proposal, LookupEnvironment env) {
        char[] declarationSignature = proposal.getDeclarationSignature();
        if (declarationSignature[0] != 'L') {
            // Should not happen. The declaring type is always a reference type.
            return absent();
        }

        int semicolonIndex = ArrayUtils.indexOf(declarationSignature, ';');
        int greaterThanIndex = ArrayUtils.indexOf(declarationSignature, '<');
        if (greaterThanIndex == ArrayUtils.INDEX_NOT_FOUND) {
            greaterThanIndex = Integer.MAX_VALUE;
        }
        char[][] compoundName = splitOn('.', declarationSignature, 1, min(semicolonIndex, greaterThanIndex));

        return lookup(env, compoundName);
    }

    /**
     * @see <a href="https://www.eclipse.org/forums/index.php/m/1410672/">Forum discussion as to why the
     *      <code>ProblemReferenceBinding</code> handling is necessary</a>
     */
    private static Optional<ReferenceBinding> lookup(LookupEnvironment env, char[][] compoundName) {
        ReferenceBinding result = env.getType(compoundName);
        if (result instanceof ProblemReferenceBinding) {
            result = cast(((ProblemReferenceBinding) result).closestMatch());
        }
        return Optional.fromNullable(result);
    }
}
