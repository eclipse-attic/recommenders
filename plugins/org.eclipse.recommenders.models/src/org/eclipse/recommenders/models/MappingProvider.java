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

import static com.google.common.base.Optional.absent;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class MappingProvider implements IMappingProvider {

    private List<IProjectCoordinateResolver> strategies = Lists.newArrayList();

    @Override
    public List<IProjectCoordinateResolver> getStrategies() {
        return ImmutableList.copyOf(strategies);
    }

    @Override
    public void addStrategy(IProjectCoordinateResolver strategy) {
        strategies.add(strategy);
    }

    @Override
    public void setStrategies(List<IProjectCoordinateResolver> strategies) {
        this.strategies = strategies;
    }

    @Override
    public Optional<ProjectCoordinate> searchForProjectCoordinate(final DependencyInfo dependencyInfo) {
        return extractProjectCoordinate(dependencyInfo);
    }

    private Optional<ProjectCoordinate> extractProjectCoordinate(DependencyInfo dependencyInfo) {
        for (IProjectCoordinateResolver strategy : strategies) {
            Optional<ProjectCoordinate> optionalProjectCoordinate = strategy.searchForProjectCoordinate(dependencyInfo);
            if (optionalProjectCoordinate.isPresent()) {
                return optionalProjectCoordinate;
            }
        }
        return absent();
    }

    @Override
    public boolean isApplicable(DependencyType dependencyTyp) {
        for (IProjectCoordinateResolver mappingStrategy : strategies) {
            if (mappingStrategy.isApplicable(dependencyTyp)) {
                return true;
            }
        }
        return false;
    }

}
