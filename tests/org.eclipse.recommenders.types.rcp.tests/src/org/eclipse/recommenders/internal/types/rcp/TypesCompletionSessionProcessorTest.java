/**
 * Copyright (c) 2015 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andreas Sewe - initial API and implementation.
 */
package org.eclipse.recommenders.internal.types.rcp;

import static org.eclipse.recommenders.internal.types.rcp.TypesCompletionSessionProcessor.BOOST;
import static org.eclipse.recommenders.testing.rcp.completion.SimpleProposalProcessorMatcher.processorWithBoost;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.recommenders.completion.rcp.IRecommendersCompletionContext;
import org.eclipse.recommenders.completion.rcp.processable.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.processable.OverlayImageProposalProcessor;
import org.eclipse.recommenders.completion.rcp.processable.ProcessableJavaCompletionProposal;
import org.eclipse.recommenders.completion.rcp.processable.ProposalProcessorManager;
import org.eclipse.recommenders.completion.rcp.processable.ProposalTag;
import org.eclipse.recommenders.rcp.SharedImages;
import org.eclipse.recommenders.utils.names.ITypeName;
import org.eclipse.recommenders.utils.names.VmTypeName;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

public class TypesCompletionSessionProcessorTest {

    private static final ITypeName LIST = VmTypeName.get("Ljava/util/List");
    private static final ITypeName SET = VmTypeName.get("Ljava/util/Set");

    private static final String ARRAY_LIST_SIGNATURE = "Ljava.util.ArrayList;";
    private static final String ARRAY_LIST_GENERIC_SIGNATURE = "Ljava.util.ArrayList<TE;>;";
    private static final String LINKED_LIST_SIGNATURE = "Ljava.util.LinkedList;";
    private static final String ABSTRACT_SET_SIGNATURE = "Ljava.util.AbstractSet;";

    @Test
    public void testNoExpectedTypes() {
        ITypesIndexService service = mockTypesIndexServer(ImmutableSetMultimap.<ITypeName, String>of());
        IRecommendersCompletionContext context = mockCompletionContextWithExpectedTypeNames();
        TypesCompletionSessionProcessor sut = new TypesCompletionSessionProcessor(service, new SharedImages());

        boolean shouldProcess = sut.startSession(context);

        assertThat(shouldProcess, is(equalTo(false)));

        verifyZeroInteractions(service);
    }

    @Test
    public void testNoProposals() throws Exception {
        ITypesIndexService service = mockTypesIndexServer(ImmutableSetMultimap.<ITypeName, String>of());
        IRecommendersCompletionContext context = mockCompletionContextWithExpectedTypeNames(LIST);
        TypesCompletionSessionProcessor sut = new TypesCompletionSessionProcessor(service, new SharedImages());

        boolean shouldProcess = sut.startSession(context);

        assertThat(shouldProcess, is(equalTo(false)));
    }

    @Test
    public void testSingleProposal() throws Exception {
        ITypesIndexService service = mockTypesIndexServer(ImmutableSetMultimap.of(LIST, "java.util.ArrayList"));
        IRecommendersCompletionContext context = mockCompletionContextWithExpectedTypeNames(LIST);

        TypesCompletionSessionProcessor sut = new TypesCompletionSessionProcessor(service, new SharedImages());

        boolean shouldProcess = sut.startSession(context);

        assertThat(shouldProcess, is(equalTo(true)));

        ProposalProcessorManager manager = mock(ProposalProcessorManager.class);
        IProcessableProposal arrayListProcessableProposal = mockProcessableProposal(manager,
                CompletionProposal.TYPE_REF, ARRAY_LIST_SIGNATURE);

        sut.process(arrayListProcessableProposal);

        verify(arrayListProcessableProposal).setTag(ProposalTag.RECOMMENDERS_SCORE, BOOST);
        verify(manager, times(1)).addProcessor(processorWithBoost(BOOST));
        verify(manager, times(1)).addProcessor(isA(OverlayImageProposalProcessor.class));
        verifyNoMoreInteractions(manager);
    }

    @Test
    public void testMultipleProposals() throws Exception {
        ITypesIndexService service = mockTypesIndexServer(
                ImmutableSetMultimap.of(LIST, "java.util.ArrayList", LIST, "java.util.LinkedList"));
        IRecommendersCompletionContext context = mockCompletionContextWithExpectedTypeNames(LIST);

        TypesCompletionSessionProcessor sut = new TypesCompletionSessionProcessor(service, new SharedImages());

        boolean shouldProcess = sut.startSession(context);

        assertThat(shouldProcess, is(equalTo(true)));

        ProposalProcessorManager linkedListProposalManager = mock(ProposalProcessorManager.class);
        IProcessableProposal linkedListProcessableProposal = mockProcessableProposal(linkedListProposalManager,
                CompletionProposal.TYPE_REF, LINKED_LIST_SIGNATURE);

        ProposalProcessorManager arrayListProposalManager = mock(ProposalProcessorManager.class);
        IProcessableProposal arrayListProcessableProposal = mockProcessableProposal(arrayListProposalManager,
                CompletionProposal.TYPE_REF, ARRAY_LIST_SIGNATURE);

        sut.process(linkedListProcessableProposal);

        verify(linkedListProcessableProposal).setTag(ProposalTag.RECOMMENDERS_SCORE, BOOST);
        verify(linkedListProposalManager, times(1)).addProcessor(processorWithBoost(BOOST));
        verify(linkedListProposalManager, times(1)).addProcessor(isA(OverlayImageProposalProcessor.class));
        verifyNoMoreInteractions(linkedListProposalManager);

        sut.process(arrayListProcessableProposal);

        verify(arrayListProcessableProposal).setTag(ProposalTag.RECOMMENDERS_SCORE, BOOST);
        verify(arrayListProposalManager, times(1)).addProcessor(processorWithBoost(BOOST));
        verify(arrayListProposalManager, times(1)).addProcessor(isA(OverlayImageProposalProcessor.class));
        verifyNoMoreInteractions(arrayListProposalManager);
    }

    @Test
    public void testMultipleExpectedTypes() throws Exception {
        ITypesIndexService service = mockTypesIndexServer(
                ImmutableSetMultimap.of(LIST, "java.util.ArrayList", SET, "java.util.AbstractSet"));
        IRecommendersCompletionContext context = mockCompletionContextWithExpectedTypeNames(LIST, SET);

        TypesCompletionSessionProcessor sut = new TypesCompletionSessionProcessor(service, new SharedImages());

        boolean shouldProcess = sut.startSession(context);

        assertThat(shouldProcess, is(equalTo(true)));

        ProposalProcessorManager arrayListProposalManager = mock(ProposalProcessorManager.class);
        IProcessableProposal arrayListProcessableProposal = mockProcessableProposal(arrayListProposalManager,
                CompletionProposal.TYPE_REF, ARRAY_LIST_SIGNATURE);

        ProposalProcessorManager abstractSetProposalManager = mock(ProposalProcessorManager.class);
        IProcessableProposal abstractSetProcessableProposal = mockProcessableProposal(abstractSetProposalManager,
                CompletionProposal.TYPE_REF, ABSTRACT_SET_SIGNATURE);

        sut.process(arrayListProcessableProposal);

        verify(arrayListProcessableProposal).setTag(ProposalTag.RECOMMENDERS_SCORE, BOOST);
        verify(arrayListProposalManager, times(1)).addProcessor(processorWithBoost(BOOST));
        verify(arrayListProposalManager, times(1)).addProcessor(isA(OverlayImageProposalProcessor.class));
        verifyNoMoreInteractions(arrayListProposalManager);

        sut.process(abstractSetProcessableProposal);

        verify(abstractSetProcessableProposal).setTag(ProposalTag.RECOMMENDERS_SCORE, BOOST);
        verify(abstractSetProposalManager, times(1)).addProcessor(processorWithBoost(BOOST));
        verify(abstractSetProposalManager, times(1)).addProcessor(isA(OverlayImageProposalProcessor.class));
        verifyNoMoreInteractions(abstractSetProposalManager);
    }

    @Test
    public void testConstructorInvocationProposalWithGenerics() throws Exception {
        ITypesIndexService service = mockTypesIndexServer(ImmutableSetMultimap.of(LIST, "java.util.ArrayList"));
        IRecommendersCompletionContext context = mockCompletionContextWithExpectedTypeNames(LIST);

        TypesCompletionSessionProcessor sut = new TypesCompletionSessionProcessor(service, new SharedImages());

        boolean shouldProcess = sut.startSession(context);

        assertThat(shouldProcess, is(equalTo(true)));

        ProposalProcessorManager manager = mock(ProposalProcessorManager.class);
        IProcessableProposal genericArrayListProcessableProposal = mockProcessableProposal(manager,
                CompletionProposal.CONSTRUCTOR_INVOCATION, ARRAY_LIST_GENERIC_SIGNATURE);

        sut.process(genericArrayListProcessableProposal);

        verify(genericArrayListProcessableProposal).setTag(ProposalTag.RECOMMENDERS_SCORE, BOOST);
        verify(manager, times(1)).addProcessor(processorWithBoost(BOOST));
        verify(manager, times(1)).addProcessor(isA(OverlayImageProposalProcessor.class));
        verifyNoMoreInteractions(manager);
    }

    @Test
    public void testAnonymousClassConstructorInvocationProposal() throws Exception {
        ITypesIndexService service = mockTypesIndexServer(ImmutableSetMultimap.of(SET, "java.util.AbstractSet"));
        IRecommendersCompletionContext context = mockCompletionContextWithExpectedTypeNames(SET);

        TypesCompletionSessionProcessor sut = new TypesCompletionSessionProcessor(service, new SharedImages());

        boolean shouldProcess = sut.startSession(context);

        assertThat(shouldProcess, is(equalTo(true)));

        ProposalProcessorManager manager = mock(ProposalProcessorManager.class);
        IProcessableProposal abstractSetProcessableProposal = mockProcessableProposal(manager,
                CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, ABSTRACT_SET_SIGNATURE);

        sut.process(abstractSetProcessableProposal);

        verify(abstractSetProcessableProposal).setTag(ProposalTag.RECOMMENDERS_SCORE, BOOST);
        verify(manager, times(1)).addProcessor(processorWithBoost(BOOST));
        verify(manager, times(1)).addProcessor(isA(OverlayImageProposalProcessor.class));
        verifyNoMoreInteractions(manager);
    }

    private IRecommendersCompletionContext mockCompletionContextWithExpectedTypeNames(ITypeName... expectedTypeNames) {
        IRecommendersCompletionContext context = mock(IRecommendersCompletionContext.class);

        when(context.getExpectedTypeNames()).thenReturn(ImmutableSet.copyOf(expectedTypeNames));

        IJavaProject project = mock(IJavaProject.class);
        when(context.getProject()).thenReturn(project);

        return context;
    }

    private ITypesIndexService mockTypesIndexServer(SetMultimap<ITypeName, String> index) {
        ITypesIndexService service = mock(ITypesIndexService.class);

        for (ITypeName typeName : index.keySet()) {
            Set<String> subtypes = index.get(typeName);
            when(service.subtypes(eq(typeName), any(String.class), any(IJavaProject.class))).thenReturn(subtypes);
        }

        return service;
    }

    private IProcessableProposal mockProcessableProposal(ProposalProcessorManager manager, int coreProposalKind,
            String coreProposalSig) {
        IProcessableProposal processableProposal = mock(ProcessableJavaCompletionProposal.class);
        when(processableProposal.getProposalProcessorManager()).thenReturn(manager);

        CompletionProposal coreProposal = mock(CompletionProposal.class);
        when(coreProposal.getKind()).thenReturn(coreProposalKind);
        when(processableProposal.getCoreProposal()).thenReturn(Optional.fromNullable(coreProposal));

        switch (coreProposalKind) {
        case CompletionProposal.TYPE_REF: {
            when(coreProposal.getSignature()).thenReturn(coreProposalSig.toCharArray());
            break;
        }
        case CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION:
        case CompletionProposal.CONSTRUCTOR_INVOCATION:
            when(coreProposal.getDeclarationSignature()).thenReturn(coreProposalSig.toCharArray());
        }

        return processableProposal;
    }
}
