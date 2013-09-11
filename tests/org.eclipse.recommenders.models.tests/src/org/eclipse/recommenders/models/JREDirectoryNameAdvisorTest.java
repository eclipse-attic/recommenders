/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andreas Sewe - initial API and implementation
 */
package org.eclipse.recommenders.models;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.recommenders.models.advisors.JREDirectoryNameAdvisor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class JREDirectoryNameAdvisorTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final DependencyInfo dependencyInfo;
    private final Optional<ProjectCoordinate> expectedCoordinate;

    public JREDirectoryNameAdvisorTest(File file, String jreVersion) throws IOException {
        dependencyInfo = new DependencyInfo(file, DependencyType.JRE);
        expectedCoordinate = Coordinates.tryNewProjectCoordinate("jre", "jre", jreVersion);
    }

    @Parameters
    public static Collection<Object[]> scenarios() throws Throwable {
        LinkedList<Object[]> scenarios = Lists.newLinkedList();
        // Real-world scenarios
        scenarios.add(jre(new String[] { "Library", "Java", "JavaVirtualMachines", "1.6.0_45-b06-451.jdk", "Contents",
                "Home" }, "1.6.0"));
        scenarios
                .add(jre(new String[] { "Library", "Java", "JavaVirtualMachines", "jdk1.7.0_09.jdk", "Contents/Home" },
                        "1.7.0"));
        scenarios.add(jre(new String[] { "usr", "lib", "jvm", "java-1.7.0-openjdk-amd64", "jre" }, "1.7.0"));
        scenarios.add(jre(new String[] { "usr", "lib", "jvm", "java-1.5.0-gcj-4.6", "jre" }, "1.5.0"));
        scenarios.add(jre(new String[] { "usr", "lib", "jvm", "java-7-openjdk-amd64", "jre" }, "7.0.0")); // "faulty"
                                                                                                          // identification
        scenarios.add(jre(new String[] { "System", "Library", "Frameworks", "JavaVM.framework", "CurrentJDK", "Home" },
                ""));

        // Artificial scenarios
        scenarios.add(jre(new String[] { "1.6.0", "..", "1.7.0" }, "1.7.0"));
        scenarios.add(jre(new String[] { "1.6.0", "..", "current" }, "1.6.0"));

        return scenarios;
    }

    private static Object[] jre(String[] dirs, String jreVersion) {
        return new Object[] { createAbsoluteFile(dirs), jreVersion };
    }

    private static File createAbsoluteFile(String... dirs) {
        File file = File.listRoots()[0];
        for (String dir : dirs) {
            file = new File(file, dir);
        }
        return file;
    }

    @Test
    public void testAdvisor() {
        IProjectCoordinateAdvisor sut = new JREDirectoryNameAdvisor();

        Optional<ProjectCoordinate> coordinate = sut.suggest(dependencyInfo);

        assertThat(coordinate, is(equalTo(expectedCoordinate)));
    }
}
