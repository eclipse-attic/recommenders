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
package org.eclipse.recommenders.tests.internal.analysis.archive;

import java.util.jar.JarFile;

import org.eclipse.recommenders.utils.archive.FilenameJarIdExtractor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class FilenameExtractionTest {

    @Test
    public void testSimpleJarFile() throws Exception {
        final JarFile file = Mockito.mock(JarFile.class);
        Mockito.when(file.getName()).thenReturn("test.name.jar");

        final FilenameJarIdExtractor extractor = new FilenameJarIdExtractor();
        extractor.extract(file);
        Assert.assertEquals("test.name", extractor.getName());
    }

}
