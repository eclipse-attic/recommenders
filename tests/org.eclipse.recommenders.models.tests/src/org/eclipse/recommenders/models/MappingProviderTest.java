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

import static com.google.common.base.Optional.fromNullable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.recommenders.models.ProjectCoordinate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class MappingProviderTest {

    private static final ProjectCoordinate EXPECTED_PROJECT_COORDINATE = new ProjectCoordinate("example",
            "example.project", "1.0.0");
    private static final ProjectCoordinate ANOTHER_EXPECTED_PROJECT_COORDINATE = new ProjectCoordinate(
            "another.example", "another.example.project", "1.2.3");

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    private File exampleFile;

    private IProjectCoordinateResolver createMockedStrategy(final ProjectCoordinate projectCoordinate,
            final DependencyType... dependencyTypes) {
        IProjectCoordinateResolver mockedStrategy = Mockito.mock(IProjectCoordinateResolver.class);
        Mockito.when(mockedStrategy.searchForProjectCoordinate(Mockito.any(DependencyInfo.class))).thenReturn(
                fromNullable(projectCoordinate));
        Mockito.when(mockedStrategy.isApplicable(Mockito.any(DependencyType.class))).thenReturn(false);
        for (DependencyType dependencyType : dependencyTypes) {
            Mockito.when(mockedStrategy.isApplicable(dependencyType)).thenReturn(true);
        }
        return mockedStrategy;
    }

    @Before
    public void init() throws IOException {
        exampleFile = folder.newFile("example.jar");
    }

    @Test
    public void testMappingProviderWithNoStrategy() throws IOException {
        IMappingProvider sut = new MappingProvider();
        Optional<ProjectCoordinate> optionalProjectCoordinate = sut.searchForProjectCoordinate(new DependencyInfo(
                exampleFile, DependencyType.JAR));

        assertFalse(optionalProjectCoordinate.isPresent());
    }

    @Test
    public void testMappingProviderWithMockedStrategy() throws IOException {
        IMappingProvider sut = new MappingProvider();
        sut.addStrategy(createMockedStrategy(EXPECTED_PROJECT_COORDINATE));
        Optional<ProjectCoordinate> optionalProjectCoordinate = sut.searchForProjectCoordinate(new DependencyInfo(
                exampleFile, DependencyType.JAR));

        assertEquals(EXPECTED_PROJECT_COORDINATE, optionalProjectCoordinate.get());
    }

    @Test
    public void testCorrectOrderOfStrategiesWithAddStrategies() throws IOException {
        IMappingProvider sut = new MappingProvider();
        sut.addStrategy(createMockedStrategy(EXPECTED_PROJECT_COORDINATE));
        sut.addStrategy(createMockedStrategy(ANOTHER_EXPECTED_PROJECT_COORDINATE));

        Optional<ProjectCoordinate> optionalProjectCoordinate = sut.searchForProjectCoordinate(new DependencyInfo(
                exampleFile, DependencyType.JAR));

        assertEquals(EXPECTED_PROJECT_COORDINATE, optionalProjectCoordinate.get());
    }

    @Test
    public void testSetStrategiesSetStrategiesCorrect() {
        IMappingProvider sut = new MappingProvider();

        List<IProjectCoordinateResolver> strategies = Lists.newArrayList();
        strategies.add(createMockedStrategy(EXPECTED_PROJECT_COORDINATE));
        strategies.add(createMockedStrategy(ANOTHER_EXPECTED_PROJECT_COORDINATE));
        sut.setStrategies(strategies);

        assertEquals(strategies, sut.getStrategies());
    }

    @Test
    public void testIsApplicableWithoutStrategies() {
        IMappingProvider sut = new MappingProvider();
        assertFalse(sut.isApplicable(DependencyType.JAR));
    }

    @Test
    public void testIsApplicableWithStrategies() {
        IMappingProvider sut = new MappingProvider();
        sut.addStrategy(createMockedStrategy(ProjectCoordinate.UNKNOWN, DependencyType.JRE));
        sut.addStrategy(createMockedStrategy(ProjectCoordinate.UNKNOWN, DependencyType.JAR));
        assertTrue(sut.isApplicable(DependencyType.JAR));
    }

    @Test
    public void testCorrectOrderOfStrategiesWithSetStrategies() throws IOException {
        IMappingProvider sut = new MappingProvider();

        List<IProjectCoordinateResolver> strategies = Lists.newArrayList();
        strategies.add(createMockedStrategy(EXPECTED_PROJECT_COORDINATE));
        strategies.add(createMockedStrategy(ANOTHER_EXPECTED_PROJECT_COORDINATE));
        sut.setStrategies(strategies);

        Optional<ProjectCoordinate> optionalProjectCoordinate = sut.searchForProjectCoordinate(new DependencyInfo(
                exampleFile, DependencyType.JAR));

        assertEquals(EXPECTED_PROJECT_COORDINATE, optionalProjectCoordinate.get());
    }

    @Test
    public void testSecondStrategyWins() throws IOException {
        IMappingProvider sut = new MappingProvider();

        List<IProjectCoordinateResolver> strategies = Lists.newArrayList();
        strategies.add(createMockedStrategy(null));
        strategies.add(createMockedStrategy(EXPECTED_PROJECT_COORDINATE));
        sut.setStrategies(strategies);

        Optional<ProjectCoordinate> optionalProjectCoordinate = sut.searchForProjectCoordinate(new DependencyInfo(
                exampleFile, DependencyType.JAR));
        assertEquals(EXPECTED_PROJECT_COORDINATE, optionalProjectCoordinate.get());
    }

}
