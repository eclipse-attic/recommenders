/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.tests.rdk.utils;

import static org.junit.Assert.assertEquals;

import org.eclipse.recommenders.rdk.utils.Artifacts;
import org.junit.Test;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;

public class ArtifactsTest {

    public static Artifact SWT_37_CALLS = new DefaultArtifact("org.eclipse.swt", "org.eclipse.swt", "cr-calls", "zip",
            "3.7.0");

    @Test
    public void utilsAsCoord() {
        String expected = "org.eclipse.swt:org.eclipse.swt:zip:cr-calls:3.7.0";
        String actual = Artifacts.asCoordinate(SWT_37_CALLS);
        assertEquals(expected, actual);
    }

    @Test
    public void utilsToArtifactFileName() {
        String actual = Artifacts.toArtifactFileName(SWT_37_CALLS);
        assertEquals("org.eclipse.swt-3.7.0-cr-calls.zip", actual);
    }

    @Test
    public void utilsNewArtifact() {
        Artifact actual = Artifacts.newClassifierAndExtension(Artifacts.pom(SWT_37_CALLS),
                SWT_37_CALLS.getClassifier(), SWT_37_CALLS.getExtension());
        assertEquals(SWT_37_CALLS, actual);

        actual = Artifacts.asArtifact(SWT_37_CALLS.toString());
        assertEquals(SWT_37_CALLS, actual);
    }

    @Test
    public void utilsPom() {
        Artifact expected = new DefaultArtifact("org.eclipse.swt:org.eclipse.swt:pom:3.7.0");
        Artifact actual = Artifacts.pom(SWT_37_CALLS);
        assertEquals(expected, actual);
    }

    @Test
    public void utilsGid() {
        String expected = "org.eclipse.recommenders";
        String actual = Artifacts.guessGroupId("org.eclipse.recommenders.extdoc.rcp");
        assertEquals(expected, actual);

        expected = "org";
        actual = Artifacts.guessGroupId("org");
        assertEquals(expected, actual);

        expected = "org.eclipse";
        actual = Artifacts.guessGroupId("org.eclipse");
        assertEquals(expected, actual);

        expected = "name.bruch";
        actual = Artifacts.guessGroupId("name.bruch");
        assertEquals(expected, actual);

        expected = "myname";
        actual = Artifacts.guessGroupId("myname.bruch");
        assertEquals(expected, actual);

    }

}
