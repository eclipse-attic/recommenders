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

import java.io.File;
import java.io.IOException;

import com.google.common.base.Optional;

public interface IModelRepository {

    /**
     * The coordinate under which the model search index of the remote model repository is addressable.
     */
    // TODO does this hve to be part of the public API?
    ModelArchiveCoordinate INDEX = new ModelArchiveCoordinate("org.eclipse.recommenders", "index", "index", "zip",
            "0.0.0-SNAPSHOT");

    /**
     * Resolves the given model coordinate to a local file and downloads the corresponding file from the remote
     * repository if not locally available.
     * 
     * @throws Exception
     *             if no model could be downloaded due to, e.g., the coordinate does not exist on the remote repository
     *             or a network/io error occurred.
     */
    void resolve(ModelArchiveCoordinate model) throws Exception;

    /**
     * Deletes the artifact represented by the given coordinate from the local file system.
     */
    void delete(ModelArchiveCoordinate model) throws IOException;

    /**
     * Returns the file for the given coordinate - if it exists. Note that this call does <b>not</b> download any
     * resources from the remote repository. It only touches the local file system.
     */
    Optional<File> getLocation(ModelArchiveCoordinate coordinate);

    /**
     * Searches the model index for all model archives matching the given {@link ProjectCoordinate} and model-type.
     */
    ModelArchiveCoordinate[] findModelArchives(ProjectCoordinate coordinate, String modelType);

    /**
     * Searches the model index for the best model archive matching the given {@link ProjectCoordinate} and model-type.
     */
    Optional<ModelArchiveCoordinate> findBestModelArchive(ProjectCoordinate coordinate, String modelType);
}
