/**
 * Copyright (c) 2010 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Johannes Lerch - initial API and implementation.
 */
package org.eclipse.recommenders.internal.rcp.codecompletion.calls;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Set;

import org.eclipse.recommenders.commons.bayesnet.BayesianNetwork;
import org.eclipse.recommenders.commons.utils.names.ITypeName;

public interface ICallsModelLoader {

    public abstract Set<ITypeName> readAvailableTypes();

    public abstract BayesianNetwork loadNetworkForTypeName(final ITypeName name, final Type returnType)
            throws IOException;

}