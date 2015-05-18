/**
 * Copyright (c) 2015 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andreas Sewe - initial API and implementation
 */
package org.eclipse.recommenders.internal.constructors.rcp;

import static java.text.MessageFormat.format;
import static java.util.Objects.requireNonNull;
import static org.eclipse.recommenders.completion.rcp.processable.ProposalTag.RECOMMENDERS_SCORE;
import static org.eclipse.recommenders.rcp.SharedImages.Images.OVR_STAR;
import static org.eclipse.recommenders.utils.Constants.REASON_NOT_IN_CACHE;
import static org.eclipse.recommenders.utils.Result.*;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.codeassist.complete.CompletionOnSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.recommenders.completion.rcp.CompletionContextKey;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.completion.rcp.processable.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.processable.OverlayImageProposalProcessor;
import org.eclipse.recommenders.completion.rcp.processable.ProposalProcessorManager;
import org.eclipse.recommenders.completion.rcp.processable.SessionProcessor;
import org.eclipse.recommenders.completion.rcp.processable.SimpleProposalProcessor;
import org.eclipse.recommenders.completion.rcp.utils.ProposalUtils;
import org.eclipse.recommenders.internal.constructors.rcp.l10n.Messages;
import org.eclipse.recommenders.internal.models.rcp.PrefetchModelArchiveJob;
import org.eclipse.recommenders.models.UniqueTypeName;
import org.eclipse.recommenders.models.rcp.IProjectCoordinateProvider;
import org.eclipse.recommenders.rcp.SharedImages;
import org.eclipse.recommenders.utils.Recommendation;
import org.eclipse.recommenders.utils.Recommendations;
import org.eclipse.recommenders.utils.Result;
import org.eclipse.recommenders.utils.names.IMethodName;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class ConstructorCompletionSessionProcessor extends SessionProcessor {

    private final ImmutableSet<Class<? extends ASTNode>> supportedCompletionRequests = ImmutableSet
            .<Class<? extends ASTNode>>of(CompletionOnSingleTypeReference.class);

    private final IProjectCoordinateProvider pcProvider;
    private final IConstructorModelProvider modelProvider;
    private final ConstructorsRcpPreferences prefs;
    private final OverlayImageProposalProcessor overlayProcessor;

    private Map<CompletionProposal, Double> recommationationsMap;

    @Inject
    public ConstructorCompletionSessionProcessor(IProjectCoordinateProvider pcProvider,
            IConstructorModelProvider modelProvider, ConstructorsRcpPreferences prefs, SharedImages images) {
        this.pcProvider = requireNonNull(pcProvider);
        this.modelProvider = requireNonNull(modelProvider);
        this.prefs = requireNonNull(prefs);
        this.overlayProcessor = new OverlayImageProposalProcessor(images.getDescriptor(OVR_STAR), IDecoration.TOP_LEFT);
    }

    @Override
    public boolean startSession(final IRecommendersCompletionContext context) {
        if (!isCompletionRequestSupported(context)) {
            return false;
        }

        LookupEnvironment env = context.get(CompletionContextKey.LOOKUP_ENVIRONMENT).orNull();
        if (env == null) {
            return false;
        }

        IType expectedType = context.getExpectedType().orNull();
        if (expectedType == null) {
            return false;
        }

        final ConstructorModel model;
        Result<UniqueTypeName> res = pcProvider.tryToUniqueName(expectedType);
        switch (res.getReason()) {
        case OK:
            model = modelProvider.acquireModel(res.get()).orNull();
            break;
        case REASON_NOT_IN_CACHE:
            new PrefetchModelArchiveJob<ConstructorModel>(expectedType, pcProvider, modelProvider).schedule(200);
            // fall-through
        case ABSENT:
        default:
            return false;
        }

        try {
            if (model == null) {
                return false;
            }

            Map<IJavaCompletionProposal, CompletionProposal> proposals = context.getProposals();
            final Map<CompletionProposal, IMethodName> foundConstructors = Maps.newHashMap();
            int runningTotal = 0;
            for (Entry<IJavaCompletionProposal, CompletionProposal> entry : proposals.entrySet()) {
                CompletionProposal coreProposal = entry.getValue();
                if (coreProposal.getKind() != CompletionProposal.CONSTRUCTOR_INVOCATION) {
                    continue;
                }
                IMethodName methodName = ProposalUtils.toMethodName(coreProposal, env).orNull();
                if (methodName == null) {
                    continue;
                }
                if (!methodName.isInit()) {
                    continue;
                }
                foundConstructors.put(coreProposal, methodName);
                runningTotal += model.getConstructorCallCount(methodName);
            }
            final int foundConstructorsTotal = runningTotal;

            if (foundConstructorsTotal == 0) {
                return false;
            }

            Iterable<Recommendation<CompletionProposal>> recommendations = Iterables.transform(
                    foundConstructors.entrySet(),
                    new Function<Entry<CompletionProposal, IMethodName>, Recommendation<CompletionProposal>>() {

                        @Override
                        public Recommendation<CompletionProposal> apply(Entry<CompletionProposal, IMethodName> entry) {
                            IMethodName methodName = entry.getValue();
                            double relevance = model.getConstructorCallCount(methodName)
                                    / (double) foundConstructorsTotal;
                            return Recommendation.newRecommendation(entry.getKey(), relevance);
                        }
                    });

            List<Recommendation<CompletionProposal>> topRecommedations = Recommendations.top(recommendations,
                    prefs.maxNumberOfProposals, prefs.minProposalProbability / 100.0);
            if (topRecommedations.isEmpty()) {
                return false;
            }

            recommationationsMap = Recommendations.asMap(topRecommedations);

            return true;
        } finally {
            modelProvider.releaseModel(model);
        }
    }

    @Override
    public void process(IProcessableProposal proposal) throws Exception {
        CompletionProposal coreProposal = proposal.getCoreProposal().orNull();
        if (coreProposal == null) {
            return;
        }

        Double relevance = recommationationsMap.get(coreProposal);
        if (relevance == null) {
            return;
        }
        final int boost = prefs.changeProposalRelevance ? 200 + relevance.intValue() : 0;
        if (boost > 0) {
            proposal.setTag(RECOMMENDERS_SCORE, relevance);
        }

        String label = ""; //$NON-NLS-1$
        if (prefs.decorateProposalText) {
            String format = relevance < 0.01d ? Messages.PROPOSAL_LABEL_PROMILLE : Messages.PROPOSAL_LABEL_PERCENTAGE;
            label = format(format, relevance);
        }

        ProposalProcessorManager manager = proposal.getProposalProcessorManager();
        manager.addProcessor(new SimpleProposalProcessor(boost, label));

        if (prefs.decorateProposalIcon) {
            manager.addProcessor(overlayProcessor);
        }
    }

    private boolean isCompletionRequestSupported(IRecommendersCompletionContext context) {
        final ASTNode node = context.getCompletionNode().orNull();
        if (node == null) {
            return false;
        } else {
            for (Class<? extends ASTNode> supportedCompletionRequest : supportedCompletionRequests) {
                if (supportedCompletionRequest.isInstance(node)) {
                    return true;
                }
            }
            return false;
        }
    }
}
