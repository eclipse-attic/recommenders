/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olav Lenz - initial API and implementation
 */
package org.eclipse.recommenders.models;

import org.eclipse.recommenders.utils.Fingerprints;

import com.google.common.base.Optional;

public class FingerprintStrategy extends AbstractStrategy {

    private final IModelIndex indexer;

    public FingerprintStrategy(IModelIndex indexer) {
        this.indexer = indexer;
    }

    @Override
    public boolean isApplicable(DependencyType dependencyType) {
        return dependencyType == DependencyType.JAR;
    }

    @Override
    protected Optional<ProjectCoordinate> doSuggest(DependencyInfo dependencyInfo) {
        String fingerprint = Fingerprints.sha1(dependencyInfo.getFile());
        return indexer.suggestProjectCoordinateByFingerprint(fingerprint);
    }
}
