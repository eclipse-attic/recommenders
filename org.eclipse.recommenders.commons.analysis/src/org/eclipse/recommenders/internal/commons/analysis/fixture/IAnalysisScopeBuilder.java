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
package org.eclipse.recommenders.internal.commons.analysis.fixture;

import com.ibm.wala.ipa.callgraph.AnalysisScope;

public interface IAnalysisScopeBuilder {
    public IAnalysisScopeBuilder buildPrimordialModules();

    public IAnalysisScopeBuilder buildApplicationModules();

    public IAnalysisScopeBuilder buildDependencyModules();

    public IAnalysisScopeBuilder buildExclusions();

    public AnalysisScope getAnalysisScope();
}
