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
package org.eclipse.recommenders.internal.calls.rcp;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.recommenders.calls.ICallModel;
import org.eclipse.recommenders.calls.ICallModelProvider;
import org.eclipse.recommenders.calls.PoolingCallModelProvider;
import org.eclipse.recommenders.models.BasedTypeName;
import org.eclipse.recommenders.models.IModelArchiveCoordinateResolver;
import org.eclipse.recommenders.models.IModelRepository;
import org.eclipse.recommenders.rcp.IRcpService;

import com.google.common.base.Optional;

public class RcpCallModelProvider implements ICallModelProvider, IRcpService {

    PoolingCallModelProvider delegate;

    @Inject
    public RcpCallModelProvider(IModelRepository repository, IModelArchiveCoordinateResolver index) {
        delegate = new PoolingCallModelProvider(repository, index);
    }

    @Override
    @PostConstruct
    public void open() throws IOException {
        delegate.open();
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public Optional<ICallModel> acquireModel(BasedTypeName key) {
        return delegate.acquireModel(key);
    }

    @Override
    public void releaseModel(ICallModel value) {
        delegate.releaseModel(value);
    }
}
