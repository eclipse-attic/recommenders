/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.overrides.rcp;

import static java.lang.String.valueOf;
import static org.eclipse.recommenders.rcp.SharedImages.OVR_STAR;
import static org.eclipse.recommenders.utils.Recommendations.asPercentage;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnFieldType;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.completion.rcp.processable.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.processable.Proposals;
import org.eclipse.recommenders.completion.rcp.processable.SessionProcessor;
import org.eclipse.recommenders.completion.rcp.processable.SimpleProposalProcessor;
import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.models.UniqueTypeName;
import org.eclipse.recommenders.models.rcp.IProjectCoordinateProvider;
import org.eclipse.recommenders.overrides.IOverrideModel;
import org.eclipse.recommenders.overrides.IOverrideModelProvider;
import org.eclipse.recommenders.rcp.JavaElementResolver;
import org.eclipse.recommenders.rcp.SharedImages;
import org.eclipse.recommenders.rcp.utils.JdtUtils;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.Recommendations;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.VmMethodName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "restriction" })
public class OverrideCompletionSessionProcessor extends SessionProcessor {

    private Logger log = LoggerFactory.getLogger(getClass());
    private IProjectCoordinateProvider pcProvider;
    private IOverrideModelProvider mProvider;
    private JavaElementResolver jdtCache;

    private IRecommendersCompletionContext ctx;
    private IType enclosingType;
    private IType supertype;
    private ProjectCoordinate pc;
    private IOverrideModel model;
    private List<Recommendation<IMethodName>> recommendations;
    private ImageDescriptor overlay;
    private OverridesRcpPreferences prefs;

    @Inject
    public OverrideCompletionSessionProcessor(IProjectCoordinateProvider pcProvider,
            IOverrideModelProvider modelProvider, final JavaElementResolver cache, SharedImages images,
            OverridesRcpPreferences prefs) {
        this.pcProvider = pcProvider;
        mProvider = modelProvider;
        jdtCache = cache;
        this.prefs = prefs;
        overlay = images.getDescriptor(OVR_STAR);
    };

    @Override
    public boolean startSession(IRecommendersCompletionContext context) {
        recommendations = null;
        ctx = context;

        if (isSupportedCompletionType() && findEnclosingType() && findSuperclass() && findProjectCoordinate()
                && hasModel()) {
            try {
                computeRecommendations();
                return true;
            } catch (Exception e) {
                log.error("An exception occured whilec omputing overrides recommendations.", e);
            } finally {
                releaseModel();
            }
        }
        return false;
    }

    private boolean isSupportedCompletionType() {
        ASTNode completionNode = ctx.getCompletionNode().orNull();
        return completionNode != null && completionNode.getClass() == CompletionOnFieldType.class;
    }

    private boolean findEnclosingType() {
        enclosingType = ctx.getEnclosingType().orNull();
        return enclosingType != null;
    }

    private boolean findSuperclass() {
        supertype = JdtUtils.findSuperclass(enclosingType).orNull();
        return supertype != null;
    }

    private boolean findProjectCoordinate() {
        pc = pcProvider.resolve(supertype).orNull();
        return pc != null;
    }

    private boolean hasModel() {
        UniqueTypeName name = new UniqueTypeName(pc, jdtCache.toRecType(supertype));
        model = mProvider.acquireModel(name).orNull();
        return model != null;
    }

    private void releaseModel() {
        if (model != null) {
            mProvider.releaseModel(model);
        }
    }

    private void computeRecommendations() throws JavaModelException {
        for (final IMethod m : enclosingType.getMethods()) {
            final IMethod superMethod = JdtUtils.findOverriddenMethod(m).orNull();
            if (superMethod != null) {
                final IMethodName recSuperMethod = jdtCache.toRecMethod(superMethod).or(VmMethodName.NULL);
                model.setObservedMethod(recSuperMethod);
            }
        }
        recommendations = Recommendations.top(model.recommendOverrides(), prefs.maxNumberOfProposals,
                prefs.minProposalProbability / 100d);
    }

    @Override
    public void process(IProcessableProposal proposal) throws Exception {
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }
        CompletionProposal coreProposal = proposal.getCoreProposal().orNull();
        if (coreProposal == null) {
            return;
        }
        final String prefix = ctx.getPrefix();
        switch (coreProposal.getKind()) {
        case CompletionProposal.METHOD_DECLARATION:
            final String signature = valueOf(coreProposal.getSignature()).replace('.', '/');
            final String name = valueOf(coreProposal.getName());
            final String propSignature = (name + signature).replaceAll("<\\.>", "");
            for (final Recommendation<IMethodName> r : recommendations) {
                IMethodName rMethod = r.getProposal();
                if (!rMethod.getName().startsWith(prefix)) {
                    continue;
                }

                final String recSignature = rMethod.getSignature();
                if (!recSignature.equals(propSignature)) {
                    continue;
                }

                // XXX rather high value but otherwise the default constructor shows up between the overrides
                // proposals
                int increment = prefs.changeProposalRelevance ? increment = 1000 + asPercentage(r) : 0;
                String label = prefs.decorateProposalText ? label = asPercentage(r) + " %" : null;
                proposal.getProposalProcessorManager().addProcessor(new SimpleProposalProcessor(increment, label));
                if (prefs.decorateProposalIcon) {
                    Proposals.overlay(proposal, overlay);
                }
                return;
            }
        }
    }
}
