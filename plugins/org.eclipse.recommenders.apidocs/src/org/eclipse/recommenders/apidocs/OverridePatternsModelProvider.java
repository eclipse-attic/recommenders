/**
 * Copyright (c) 2011 Stefan Henss.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Henss - initial API and implementation.
 */
package org.eclipse.recommenders.apidocs;

import static com.google.common.base.Optional.of;
import static org.eclipse.recommenders.utils.Constants.*;

import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;

import org.eclipse.recommenders.models.IInputStreamTransformer;
import org.eclipse.recommenders.models.IModelIndex;
import org.eclipse.recommenders.models.IModelRepository;
import org.eclipse.recommenders.models.PoolingModelProvider;
import org.eclipse.recommenders.models.UniqueTypeName;
import org.eclipse.recommenders.utils.IOUtils;
import org.eclipse.recommenders.utils.Zips;
import org.eclipse.recommenders.utils.gson.GsonUtil;

import com.google.common.base.Optional;

public class OverridePatternsModelProvider extends PoolingModelProvider<UniqueTypeName, ClassOverridePatterns> {

    public OverridePatternsModelProvider(IModelRepository repository, IModelIndex index,
            List<IInputStreamTransformer> transformers) {
        super(repository, index, CLASS_OVRP_MODEL, transformers);
    }

    @Override
    protected Optional<ClassOverridePatterns> loadModel(InputStream is, UniqueTypeName key) throws Exception {
        ClassOverridePatterns res = GsonUtil.deserialize(is, ClassOverridePatterns.class);
        IOUtils.closeQuietly(is);
        return of(res);
    }

    @Override
    protected ZipEntry getEntry(UniqueTypeName key) {
        return new ZipEntry(Zips.path(key.getName(), DOT_JSON));
    }
}
