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
package org.eclipse.recommenders.models;

import org.eclipse.recommenders.coordinates.ProjectCoordinate;
import org.eclipse.recommenders.utils.names.ITypeName;

/**
 * Represents an {@link ITypeName} qualified by a {@link ProjectCoordinate} like <i>jre:jre:1.6<i>. Project coordinates
 * are required to find the right recommendation model for the given type. It's in the responsibility of the recommender
 * to qualify the type it wants to make recommendations for.
 */
public class UniqueTypeName extends AbstractUniqueName<ITypeName> {

    public UniqueTypeName(ProjectCoordinate pc, ITypeName name) {
        super(pc, name);
    }
}
