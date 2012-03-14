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
package org.eclipse.recommenders.internal.completion.rcp.calls.models;

import java.io.InputStream;

import org.eclipse.recommenders.utils.names.ITypeName;

public interface IModelLoader<T extends IModel> {

    T load(final ITypeName name, final InputStream inputStream);
}
