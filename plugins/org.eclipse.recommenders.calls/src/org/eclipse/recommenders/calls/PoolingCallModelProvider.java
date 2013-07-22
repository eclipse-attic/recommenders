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
package org.eclipse.recommenders.calls;

import static org.eclipse.recommenders.utils.Constants.CLASS_CALL_MODELS;

import java.util.zip.ZipFile;

import org.eclipse.recommenders.models.BasedTypeName;
import org.eclipse.recommenders.models.IModelRepository;
import org.eclipse.recommenders.models.PoolingModelProvider;

import com.google.common.base.Optional;

public class PoolingCallModelProvider extends PoolingModelProvider<BasedTypeName, ICallModel> implements
        ICallModelProvider {

    public PoolingCallModelProvider(IModelRepository repo) {
        super(repo, CLASS_CALL_MODELS);
    }

    @Override
    protected Optional<ICallModel> loadModel(ZipFile zip, BasedTypeName key) throws Exception {
        return JayesCallModel.load(zip, key.getName());
    }
}
