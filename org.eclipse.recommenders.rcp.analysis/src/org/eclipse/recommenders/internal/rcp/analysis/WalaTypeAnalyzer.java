/**
 * Copyright (c) 2010 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.rcp.analysis;

import static org.eclipse.recommenders.commons.utils.Checks.ensureIsNotNull;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.recommenders.commons.utils.Throws;
import org.eclipse.recommenders.commons.utils.names.IMethodName;
import org.eclipse.recommenders.internal.commons.analysis.analyzers.IClassAnalyzer;
import org.eclipse.recommenders.internal.commons.analysis.codeelements.MethodDeclaration;
import org.eclipse.recommenders.internal.commons.analysis.codeelements.TypeDeclaration;
import org.eclipse.recommenders.internal.commons.analysis.entrypoints.IEntrypointSelector;
import org.eclipse.recommenders.internal.commons.analysis.utils.WalaNameUtils;
import org.eclipse.recommenders.rcp.analysis.IClassHierarchyService;
import org.eclipse.recommenders.rcp.utils.JavaElementResolver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;

public class WalaTypeAnalyzer {
    private final Set<IClassAnalyzer> classAnalyzers;

    private final Provider<WalaMethodAnalyzer> walaMethodAnalyzers;

    private final Provider<IEntrypointSelector> entrypointSelector;

    private IType jdtType;

    private IClass walaClass;

    private TypeDeclaration recType;

    private final IClassHierarchyService cha;

    private final Provider<WalaTypeAnalyzer> typeAnalyzers;

    private final JavaElementResolver jdtResolver;

    private IProgressMonitor monitor;

    @Inject
    public WalaTypeAnalyzer(final Set<IClassAnalyzer> classAnalyzers,
            final Provider<IEntrypointSelector> entrypointSelector, final Provider<WalaTypeAnalyzer> typeAnalyzers,
            final Provider<WalaMethodAnalyzer> methodAnalyzers, final IClassHierarchyService cha,
            final JavaElementResolver jdtResolver) {
        this.classAnalyzers = classAnalyzers;
        this.entrypointSelector = entrypointSelector;
        this.typeAnalyzers = typeAnalyzers;
        this.walaMethodAnalyzers = methodAnalyzers;
        this.cha = cha;
        this.jdtResolver = jdtResolver;
    }

    public void initialize(final IType jdtType, final TypeDeclaration recType, final IClass walaClass) {
        ensureIsNotNull(jdtType);
        ensureIsNotNull(walaClass);
        ensureIsNotNull(recType);
        this.jdtType = jdtType;
        this.recType = recType;
        this.walaClass = walaClass;
    }

    public void run(final IProgressMonitor monitor) throws JavaModelException {
        this.monitor = monitor;
        analyzeClassStructure();
        analyzeEachMethod();
        analyzeMemberTypes();
    }

    private void analyzeMemberTypes() throws JavaModelException {
        for (final IType jdtMemberType : jdtType.getTypes()) {
            final TypeDeclaration recMemberType = TypeDeclaration.create();
            recType.memberTypes.add(recMemberType);
            final IClass walaClass = cha.getType(jdtMemberType);
            if (walaClass != null) {
                final WalaTypeAnalyzer walaMemberTypeAnalyzer = typeAnalyzers.get();
                walaMemberTypeAnalyzer.initialize(jdtMemberType, recMemberType, walaClass);
                walaMemberTypeAnalyzer.run(monitor);
            }
        }
    }

    private void analyzeClassStructure() {
        for (final IClassAnalyzer classAnalyzer : classAnalyzers) {
            classAnalyzer.analyzeClass(walaClass, recType, monitor);
        }
    }

    private void analyzeEachMethod() throws JavaModelException {
        if (Thread.currentThread().isInterrupted() || monitor.isCanceled()) {
            Throws.throwCancelationException();
        }
        final List<Entrypoint> entrypoints = entrypointSelector.get().selectEntrypoints(walaClass);
        for (final Entrypoint entrypoint : entrypoints) {
            cancelAnalysisIfIInterrupted();
            final IMethod walaMethod = entrypoint.getMethod();
            final org.eclipse.jdt.core.IMethod jdtMethod = findJdtMethod(walaMethod);
            final MethodDeclaration recMethod = MethodDeclaration.create();
            recType.methods.add(recMethod);
            final WalaMethodAnalyzer walaMethodAnalyzer = walaMethodAnalyzers.get();
            walaMethodAnalyzer.initalize(entrypoint, jdtMethod, recMethod);
            walaMethodAnalyzer.run(monitor);
        }
    }

    private org.eclipse.jdt.core.IMethod findJdtMethod(final IMethod walaMethod) {
        ensureIsNotNull(walaMethod);
        final IMethodName recMethodName = WalaNameUtils.wala2recMethodName(walaMethod);
        final org.eclipse.jdt.core.IMethod jdtMethod = jdtResolver.toJdtMethod(recMethodName);
        return jdtMethod;
    }

    private void cancelAnalysisIfIInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            Throws.throwCancelationException();
        }
    }
}
