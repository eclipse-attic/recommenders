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
package org.eclipse.recommenders.tests.models.utils;

import java.io.File;

public final class FolderUtils {

    private FolderUtils() {
        // Not meant to be instantiated
    }

    public static File dir(String... dirs) {
        File file = File.listRoots()[0];
        for (String dir : dirs) {
            file = new File(file, dir);
        }
        return file;
    }
}
