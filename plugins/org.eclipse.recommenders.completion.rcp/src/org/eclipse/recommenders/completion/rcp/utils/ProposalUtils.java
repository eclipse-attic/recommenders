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
import static org.eclipse.jdt.core.CompletionProposal.*;
import static org.eclipse.recommenders.rcp.utils.ReflectionUtils.getDeclaredField;
import static org.eclipse.recommenders.utils.Logs.log;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.internal.codeassist.InternalCompletionProposal;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.recommenders.internal.rcp.LogMessages;
import org.eclipse.recommenders.rcp.utils.CompilerBindings;
import org.eclipse.recommenders.utils.Nullable;
import org.eclipse.recommenders.utils.names.IMethodName;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

@SuppressWarnings("restriction")
public class ProposalUtils {

    /**
     * Workaround needed to handle proposals with generic signatures properly. See
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=380203
     */
    private static Field ORIGINAL_SIGNATURE = getDeclaredField(InternalCompletionProposal.class, "originalSignature")
            .orNull();

    private static char[] getSignature(CompletionProposal proposal) {
        char[] signature = null;
        try {
            if (canUseReflection(proposal)) {
                signature = (char[]) ORIGINAL_SIGNATURE.get(proposal);
            }
        } catch (Exception e) {
            log(LogMessages.LOG_WARNING_REFLECTION_FAILED, e, ORIGINAL_SIGNATURE);
        }
        return signature != null ? signature : proposal.getSignature();
    }

    private static boolean canUseReflection(CompletionProposal proposal) {
        return proposal instanceof InternalCompletionProposal && ORIGINAL_SIGNATURE != null
                && ORIGINAL_SIGNATURE.isAccessible();
    }

    public static Optional<IMethodName> toMethodName(CompletionProposal proposal, @Nullable LookupEnvironment env) {
        Preconditions.checkArgument(proposal.getKind() == METHOD_REF
                || proposal.getKind() == METHOD_REF_WITH_CASTED_RECEIVER || proposal.getKind() == METHOD_DECLARATION);

        if (env == null) {
            return absent();
        }

        ReferenceBinding declaringType = getDeclaringType(proposal, env).orNull();
        if (declaringType == null) {
            return absent();
        }

        // TODO When we handle constructors, we need to take case that getName returns "ClassName" rather than "<init>"
        MethodBinding[] overloads = declaringType.getMethods(proposal.getName());

        char[] proposalSignature = getSignature(proposal);
        for (int i = 0; i < proposalSignature.length; i++) {
            if (proposalSignature[i] == '.') {
                proposalSignature[i] = '/';
            }
        }

        for (MethodBinding overload : overloads) {
            char[] signature = overload.genericSignature();
            if (signature == null) {
                signature = overload.signature();
            }

            if (Arrays.equals(proposalSignature, signature)) {
                return CompilerBindings.toMethodName(overload);
            }
        }

        return absent();
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
        String valueOf = String.valueOf(declarationSignature, 1, Math.min(semicolonIndex, greaterThanIndex) - 1);

        String[] split = StringUtils.split(valueOf, '.');
        char[][] compoundName = new char[split.length][];
        for (int i = 0; i < compoundName.length; i++) {
            compoundName[i] = split[i].toCharArray();
        }

        return Optional.fromNullable(env.getType(compoundName));
    }
}
