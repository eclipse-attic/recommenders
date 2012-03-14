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
package org.eclipse.recommenders.internal.completion.rcp.calls.models;

import static org.eclipse.recommenders.utils.Checks.ensureExists;
import static org.eclipse.recommenders.utils.Checks.ensureIsNotNull;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.eclipse.recommenders.utils.Checks;
import org.eclipse.recommenders.utils.Throws;
import org.eclipse.recommenders.utils.names.ITypeName;

public class ModelArchive<T extends IModel> implements IModelArchive<T> {

    private static final String MANIFEST_FILENAME = "manifest.json";
    private static final String MODEL_POSTFIX = ".data";

    private final IModelLoader<T> modelLoader;
    private ZipFile zipFile;
    // private Manifest manifest;
    private File file;

    private final GenericKeyedObjectPool pool = createPool();

    private GenericKeyedObjectPool createPool() {
        final GenericKeyedObjectPool pool = new GenericKeyedObjectPool(new CallsModelPoolFactory());
        pool.setMaxTotal(100);
        pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL);
        return pool;
    }

    public ModelArchive(final File file, final IModelLoader<T> modelLoader) {
        ensureExists(file);
        this.file = ensureIsNotNull(file);
        this.modelLoader = ensureIsNotNull(modelLoader);
        open();
        // readManifest();
    }

    @Override
    public void open() {
        try {
            zipFile = new ZipFile(file);
        } catch (final Exception e) {
            Throws.throwUnhandledException(e, "Unable to read archive '%s'", file.getAbsolutePath());
        }
    }

    // private void readManifest() {
    // try {
    // final ZipEntry manifestEntry = zipFile.getEntry(MANIFEST_FILENAME);
    // ensureIsNotNull(manifestEntry, "Archive '%s' does not contain manifest file.", file.getAbsolutePath());
    // final InputStream inputStream = zipFile.getInputStream(manifestEntry);
    // manifest = GsonUtil.deserialize(inputStream, Manifest.class);
    // } catch (final IOException e) {
    // Throws.throwUnhandledException(e, "Unable to load manifest from archive '%s'", file.getAbsolutePath());
    // }
    // }

    // @Override
    // public Manifest getManifest() {
    // return manifest;
    // }

    private String getFilenameFromType(final ITypeName type) {
        return type.getIdentifier().replaceAll("/", ".") + MODEL_POSTFIX;
    }

    @Override
    public boolean hasModel(final ITypeName name) {
        return zipFile.getEntry(getFilenameFromType(name)) != null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T acquireModel(final ITypeName name) {
        Checks.ensureIsTrue(hasModel(name));
        try {
            return (T) pool.borrowObject(name);
        } catch (final Exception e) {
            throw Throws.throwUnhandledException(e);
        }
    }

    @Override
    public void releaseModel(final T model) {
        try {
            pool.returnObject(model.getType(), model);
        } catch (final Exception e) {
            Throws.throwUnhandledException(e);
        }
    }

    private T loadModel(final ITypeName name) {
        final ZipEntry entry = zipFile.getEntry(getFilenameFromType(name));
        try {
            return modelLoader.load(name, zipFile.getInputStream(entry));
        } catch (final IOException e) {
            throw Throws.throwUnhandledException(e, "Unable to load model for type '%s' from file '%s'", name,
                    file.getAbsolutePath());
        }
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(final File file) {
        this.file = file;
    }

    private class CallsModelPoolFactory implements KeyedPoolableObjectFactory {
        @Override
        public boolean validateObject(final Object arg0, final Object arg1) {
            return true;
        }

        @Override
        public void passivateObject(final Object arg0, final Object arg1) throws Exception {
        }

        @Override
        public Object makeObject(final Object key) throws Exception {
            return loadModel((ITypeName) key);
        }

        @Override
        public void destroyObject(final Object arg0, final Object arg1) throws Exception {
        }

        @SuppressWarnings("unchecked")
        @Override
        public void activateObject(final Object typeName, final Object net) throws Exception {
            ((T) net).clearEvidence();
        }
    }

}
