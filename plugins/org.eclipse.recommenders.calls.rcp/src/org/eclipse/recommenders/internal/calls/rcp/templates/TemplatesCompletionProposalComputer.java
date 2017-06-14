/**
 * Copyright (c) 2010 Stefan Henss.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Henss - initial API and implementation.
 */
package org.eclipse.recommenders.internal.calls.rcp.templates;

import static com.google.common.collect.Sets.newHashSet;
import static org.eclipse.recommenders.completion.rcp.CompletionContextKey.ENCLOSING_METHOD_FIRST_DECLARATION;
import static org.eclipse.recommenders.internal.calls.rcp.CallCompletionContextFunctions.*;
import static org.eclipse.recommenders.internal.calls.rcp.Constants.TEMPLATES_CATEGORY_ID;
import static org.eclipse.recommenders.internal.calls.rcp.l10n.LogMessages.*;
import static org.eclipse.recommenders.utils.Logs.log;
import static org.eclipse.recommenders.utils.Recommendations.top;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.CharUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.codeassist.ISearchRequestor;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnMemberAccess;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnQualifiedNameReference;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.core.DefaultWorkingCopyOwner;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.SearchableEnvironment;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.recommenders.calls.ICallModel;
import org.eclipse.recommenders.calls.ICallModel.DefinitionKind;
import org.eclipse.recommenders.calls.ICallModelProvider;
import org.eclipse.recommenders.completion.rcp.CompletionContextKey;
import org.eclipse.recommenders.completion.rcp.DisableContentAssistCategoryJob;
import org.eclipse.recommenders.completion.rcp.ICompletionContextFunction;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.completion.rcp.RecommendersCompletionContext;
import org.eclipse.recommenders.coordinates.ProjectCoordinate;
import org.eclipse.recommenders.models.UniqueTypeName;
import org.eclipse.recommenders.models.rcp.IProjectCoordinateProvider;
import org.eclipse.recommenders.rcp.IAstProvider;
import org.eclipse.recommenders.rcp.JavaElementResolver;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.Recommendations;
import org.eclipse.recommenders.utils.Throws;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Controls the process of template recommendations.
 */
@SuppressWarnings({ "restriction", "rawtypes", "unchecked" })
public class TemplatesCompletionProposalComputer implements IJavaCompletionProposalComputer {

    public static enum CompletionMode {
        TYPE_NAME,
        MEMBER_ACCESS,
        THIS
    }

    private final Provider<IProjectCoordinateProvider> pcProvider;
    private final Provider<ICallModelProvider> store;
    private final IAstProvider astProvider;
    private final JavaElementResolver elementResolver;
    private final Map<CompletionContextKey, ICompletionContextFunction> functions;

    private IRecommendersCompletionContext rCtx;
    private IMethod enclosingMethod;
    private Set<IType> candidates;
    private String variableName;
    private boolean requiresConstructor;
    private String methodPrefix;
    private CompletionMode mode;
    private Image icon;
    private ICallModel model;

    @Inject
    public TemplatesCompletionProposalComputer(Provider<IProjectCoordinateProvider> pcProvider,
            Provider<ICallModelProvider> store, IAstProvider astProvider, JavaElementResolver elementResolver,
            Map<CompletionContextKey, ICompletionContextFunction> functions) {
        this.pcProvider = pcProvider;
        this.store = store;
        this.astProvider = astProvider;
        this.elementResolver = elementResolver;
        this.functions = functions;
        loadImage();
    }

    private void loadImage() {
        Bundle bundle = FrameworkUtil.getBundle(TemplatesCompletionProposalComputer.class);
        icon = null;
        if (bundle != null) {
            ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(bundle.getSymbolicName(),
                    "icons/view16/templates-dots.gif"); //$NON-NLS-1$
            icon = desc.createImage();
        }
    }

    @VisibleForTesting
    public CompletionMode getCompletionMode() {
        return mode;
    }

    @VisibleForTesting
    public String getVariableName() {
        return variableName;
    }

    @VisibleForTesting
    public String getMethodPrefix() {
        return methodPrefix;
    }

    @Override
    public List computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
        if (!shouldMakeProposals()) {
            return Collections.EMPTY_LIST;
        }

        rCtx = new RecommendersCompletionContext((JavaContentAssistInvocationContext) context, astProvider, functions);
        if (!findEnclosingMethod()) {
            return Collections.emptyList();
        }
        if (!findCompletionMode()) {
            return Collections.emptyList();
        }
        if (!findPotentialTypes()) {
            return Collections.emptyList();
        }

        ProposalBuilder proposalBuilder = new ProposalBuilder(icon, rCtx, elementResolver, variableName);
        for (IType t : candidates) {
            addPatternsForType(t, proposalBuilder);
        }
        return proposalBuilder.createProposals();
    }

    @VisibleForTesting
    protected boolean shouldMakeProposals() {
        // we only make proposals on non-default content assist lists (2nd, 3rd,...)
        String[] excluded = PreferenceConstants.getExcludedCompletionProposalCategories();
        Set<String> ex = Sets.newHashSet(excluded);
        if (!ex.contains(TEMPLATES_CATEGORY_ID)) {
            new DisableContentAssistCategoryJob(TEMPLATES_CATEGORY_ID).schedule();
            return false;
        }
        // we are not on the default tab
        return true;
    }

    private void addPatternsForType(IType t, ProposalBuilder proposalBuilder) {
        ProjectCoordinate pc = pcProvider.get().resolve(t).or(ProjectCoordinate.UNKNOWN);
        UniqueTypeName name = new UniqueTypeName(pc, elementResolver.toRecType(t));
        model = store.get().acquireModel(name).orNull();
        try {
            if (model == null) {
                return;
            }
            model.setObservedCalls(new HashSet<IMethodName>());
            if (mode == CompletionMode.TYPE_NAME) {
                handleTypeNameCompletionRequest(proposalBuilder);
            } else {
                handleVariableCompletionRequest(proposalBuilder);
            }
        } finally {
            store.get().releaseModel(model);
        }
    }

    private void handleVariableCompletionRequest(ProposalBuilder proposalBuilder) {
        // set override-context:
        IMethod overrides = rCtx.get(ENCLOSING_METHOD_FIRST_DECLARATION, null);
        if (overrides != null) {
            IMethodName crOverrides = elementResolver.toRecMethod(overrides)
                    .or(org.eclipse.recommenders.utils.Constants.UNKNOWN_METHOD);
            model.setObservedOverrideContext(crOverrides);
        }

        // set definition-type and defined-by
        model.setObservedDefinitionKind(rCtx.get(RECEIVER_DEF_TYPE, null));
        model.setObservedDefiningMethod(rCtx.get(RECEIVER_DEF_BY, null));

        // set calls:
        model.setObservedCalls(newHashSet(rCtx.get(RECEIVER_CALLS, Collections.<IMethodName>emptyList())));

        List<Recommendation<String>> callgroups = getMostLikelyPatternsSortedByProbability(model);
        for (Recommendation<String> p : callgroups) {
            String patternId = p.getProposal();
            model.setObservedPattern(patternId);
            Collection<IMethodName> calls = new TreeSet<>();
            for (Recommendation<IMethodName> r : top(model.recommendCalls(), 100, 0.1d)) {
                calls.add(r.getProposal());
            }
            // patterns with less than two calls are no patterns :)
            if (calls.size() < 2) {
                continue;
            }
            PatternRecommendation pattern = new PatternRecommendation(patternId, model.getReceiverType(),
                    ImmutableSet.copyOf(calls), p.getRelevance());
            proposalBuilder.addPattern(pattern);
            // }
        }
    }

    private void handleTypeNameCompletionRequest(ProposalBuilder proposalBuilder) {
        IMethod overrides = rCtx.get(ENCLOSING_METHOD_FIRST_DECLARATION, null);
        model.setObservedDefinitionKind(DefinitionKind.NEW);
        if (overrides != null) {
            IMethodName crOverrides = elementResolver.toRecMethod(overrides)
                    .or(org.eclipse.recommenders.utils.Constants.UNKNOWN_METHOD);
            model.setObservedOverrideContext(crOverrides);
        }

        List<Recommendation<String>> callgroups = getMostLikelyPatternsSortedByProbability(model);
        for (Recommendation<String> p : callgroups) {
            String patternId = p.getProposal();
            model.setObservedPattern(patternId);
            for (Recommendation<IMethodName> def : Recommendations.top(model.recommendDefinitions(), 100, 0.01d)) {
                IMethodName constructor = def.getProposal();
                // TODO XXX this looks like a bug in some mining code: we have wring receiver methods here: new
                // Label(Composite) for instance occurred in Composite model. Why?
                if (!constructor.isInit() || constructor.getDeclaringType() != model.getReceiverType()) {
                    continue;
                }
                Collection<IMethodName> calls = getCallsForDefinition(model, constructor);
                calls.add(constructor);
                // patterns with less than two calls are no patterns :)
                if (calls.size() < 2) {
                    continue;
                }

                PatternRecommendation pattern = new PatternRecommendation(patternId, model.getReceiverType(),
                        ImmutableSet.copyOf(calls), p.getRelevance());
                proposalBuilder.addPattern(pattern);
            }
        }
    }

    private Collection<IMethodName> getCallsForDefinition(ICallModel model, IMethodName definition) {
        boolean constructorAdded = false;

        TreeSet<IMethodName> calls = new TreeSet<>();
        List<Recommendation<IMethodName>> rec = Recommendations.top(model.recommendCalls(), 100, 0.1d);
        if (rec.isEmpty()) {
            return new LinkedList<>();
        }
        if (requiresConstructor && definition.isInit()) {
            calls.add(definition);
            constructorAdded = true;
        }
        if (requiresConstructor && !constructorAdded) {
            return new LinkedList<>();
        }
        for (Recommendation<IMethodName> pair : rec) {
            calls.add(pair.getProposal());
        }
        if (!containsCallWithMethodPrefix(calls)) {
            return new LinkedList<>();
        }
        return calls;
    }

    private boolean containsCallWithMethodPrefix(Set<IMethodName> calls) {
        for (IMethodName call : calls) {
            if (call.getName().startsWith(methodPrefix)) {
                return true;
            }
        }
        return false;
    }

    private List<Recommendation<String>> getMostLikelyPatternsSortedByProbability(ICallModel net) {
        return Recommendations.top(net.recommendPatterns(), 10, 0.03d);
    }

    private boolean findCompletionMode() {
        variableName = ""; //$NON-NLS-1$
        methodPrefix = ""; //$NON-NLS-1$
        mode = null;

        ASTNode n = rCtx.getCompletionNode().orNull();
        if (n instanceof CompletionOnSingleNameReference) {
            if (isPotentialClassName((CompletionOnSingleNameReference) n)) {
                mode = CompletionMode.TYPE_NAME;
            } else {
                // eq$ --> receiver is this
                mode = CompletionMode.THIS;
                methodPrefix = rCtx.getReceiverName();
            }
        } else if (n instanceof CompletionOnQualifiedNameReference) {
            if (isPotentialClassName((CompletionOnQualifiedNameReference) n)) {
                mode = CompletionMode.TYPE_NAME;
            } else {
                mode = CompletionMode.MEMBER_ACCESS;
                variableName = rCtx.getReceiverName();
                methodPrefix = rCtx.getPrefix();
            }
        } else if (n instanceof CompletionOnMemberAccess) {
            Expression ma = ((CompletionOnMemberAccess) n).receiver;
            if (ma.isImplicitThis() || ma.isSuper() || ma.isThis()) {
                mode = CompletionMode.THIS;
            } else {
                mode = CompletionMode.MEMBER_ACCESS;
            }
        }
        return mode != null;
    }

    private boolean findPotentialTypes() {
        if (mode == CompletionMode.TYPE_NAME) {
            ASTNode n = rCtx.getCompletionNode().orNull();
            CompletionOnSingleNameReference c = null;
            if (n instanceof CompletionOnSingleNameReference) {
                c = (CompletionOnSingleNameReference) n;
                candidates = findTypesBySimpleName(c.token);
            }
        } else if (mode == CompletionMode.THIS) {
            createCandidatesFromOptional(getSupertypeOfThis());
        } else {
            createCandidatesFromOptional(rCtx.getReceiverType());
        }
        return candidates != null;
    }

    private Optional<IType> getSupertypeOfThis() {
        IMethod m = rCtx.getEnclosingMethod().orNull();
        try {
            if (m == null || JdtFlags.isStatic(m)) {
                return Optional.absent();
            }
            IType type = m.getDeclaringType();
            ITypeHierarchy hierarchy = SuperTypeHierarchyCache.getTypeHierarchy(type);
            return Optional.fromNullable(hierarchy.getSuperclass(type));
        } catch (JavaModelException e) {
            log(WARNING_FAILED_TO_RESOLVE_SUPER_TYPE, e, m);
            return Optional.absent();
        } catch (Exception e) {
            log(ERROR_FAILED_TO_RESOLVE_SUPER_TYPE, e, m);
            return Optional.absent();
        }
    }

    private void createCandidatesFromOptional(Optional<IType> optType) {
        if (optType.isPresent()) {
            candidates = Sets.newHashSet(optType.get());
        }
    }

    private boolean isPotentialClassName(CompletionOnQualifiedNameReference c) {
        char[] name = c.completionIdentifier;
        return name != null && name.length > 0 && CharUtils.isAsciiAlphaUpper(name[0]);
    }

    private boolean isPotentialClassName(CompletionOnSingleNameReference c) {
        return c.token != null && c.token.length > 0 && CharUtils.isAsciiAlphaUpper(c.token[0]);
    }

    private boolean findEnclosingMethod() {
        enclosingMethod = rCtx.getEnclosingMethod().orNull();
        return enclosingMethod != null;
    }

    @Override
    public void sessionStarted() {
        // This particular event is not of interest for us.
    }

    @Override
    public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
            IProgressMonitor monitor) {
        return Collections.emptyList();
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public void sessionEnded() {
        // This particular event is not of interest for us.
    }

    public Set<IType> findTypesBySimpleName(char[] simpleTypeName) {
        final Set<IType> result = new HashSet<>();
        try {
            final JavaProject project = (JavaProject) rCtx.getProject();
            SearchableEnvironment environment = project.newSearchableNameEnvironment(DefaultWorkingCopyOwner.PRIMARY);
            environment.findExactTypes(simpleTypeName, false, IJavaSearchConstants.TYPE, new ISearchRequestor() {
                @Override
                public void acceptConstructor(int modifiers, char[] simpleTypeName, int parameterCount,
                        char[] signature, char[][] parameterTypes, char[][] parameterNames, int typeModifiers,
                        char[] packageName, int extraFlags, String path, AccessRestriction access) {
                }

                @Override
                public void acceptType(char[] packageName, char[] typeName, char[][] enclosingTypeNames, int modifiers,
                        AccessRestriction accessRestriction) {
                    IType res;
                    try {
                        res = project.findType(String.valueOf(packageName), String.valueOf(typeName));
                        if (res != null) {
                            result.add(res);
                        }
                    } catch (JavaModelException e) {
                        log(ERROR_FAILED_TO_FIND_TYPE, e, packageName, typeName);
                    }
                }

                @Override
                public void acceptPackage(char[] packageName) {
                }
            });
        } catch (JavaModelException e) {
            Throws.throwUnhandledException(e);
        }
        return result;
    }
}
