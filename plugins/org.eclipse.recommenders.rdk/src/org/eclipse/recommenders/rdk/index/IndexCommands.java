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
package org.eclipse.recommenders.rdk.index;

import static org.eclipse.recommenders.utils.Checks.ensureExists;

import java.io.File;

import org.apache.felix.service.command.Descriptor;
import org.eclipse.recommenders.rdk.utils.Commands.CommandProvider;
import org.eclipse.recommenders.utils.annotations.Provisional;

/**
 * Provides commands to create and maintain a model repository.
 * <p>
 * TODO this class is yet not ready
 * </p>
 */
@CommandProvider
@Provisional
public class IndexCommands {

    @Descriptor("generates the model index from a index file")
    public void generateModelIndex(@Descriptor("the recommenders model repository") File crBasedir) throws Exception {
        File indexJson = new File(crBasedir, "org/eclipse/recommenders/index/0.0.0/index-0.0.0.json");
        File indexZip = new File(crBasedir, "org/eclipse/recommenders/index/0.0.0/index-0.0.0.zip");
        collectModelData(crBasedir, indexJson);
        writeModelIndex(indexJson, indexZip);
    }

    @Descriptor("")
    public void writeModelIndex(@Descriptor("the model metadata index stored as json file") File in,
            @Descriptor("the destination of the zip file") File out) throws Exception {
        ensureExists(in);
        new SearchIndexWriter(in, out).run();
    }

    @Descriptor("")
    public void collectModelData(@Descriptor("the cr model repository basedir") File basedir,
            @Descriptor("the destination of the metadata file") File out) throws Exception {
        new ModelDocumentsWriter(basedir, out).run();
    }

}
