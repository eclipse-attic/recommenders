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
package org.eclipse.recommenders.internal.rcp.codecompletion.calls.store;

import java.util.Set;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.recommenders.commons.utils.Version;

import com.google.common.collect.Sets;

public class LibraryIdentifierResolverJob extends WorkspaceJob {

    private final IPackageFragmentRoot[] packageRoots;
    private final CallsModelIndex callsModelIndex;

    public LibraryIdentifierResolverJob(final CallsModelIndex callsModelIndex, final IPackageFragmentRoot[] packageRoots) {
        super("Resolving name and version of project dependencies");
        this.callsModelIndex = callsModelIndex;
        this.packageRoots = packageRoots;
    }

    @Override
    public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
        final Set<IPackageFragmentRoot> unresolvedPackageRoots = getUnresolvedPackageRoots();
        resolve(unresolvedPackageRoots);
        return Status.OK_STATUS;
    }

    private Set<IPackageFragmentRoot> getUnresolvedPackageRoots() {
        final Set<IPackageFragmentRoot> unresolvedFragmentRoots = Sets.newHashSet();

        for (final IPackageFragmentRoot packageRoot : packageRoots) {
            if (!callsModelIndex.isLibraryIdentifierResolved(packageRoot)) {
                unresolvedFragmentRoots.add(packageRoot);
            }
        }
        return unresolvedFragmentRoots;
    }

    private void resolve(final Set<IPackageFragmentRoot> unresolvedPackageRoots) {
        for (final IPackageFragmentRoot packageRoot : unresolvedPackageRoots) {
            resolve(packageRoot);
        }
    }

    private void resolve(final IPackageFragmentRoot packageRoot) {
        // TODO: This is not a real implementation we want to work with
        if (packageRoot instanceof JarPackageFragmentRoot) {
            final String name = packageRoot.getPath().lastSegment();
            if (name.matches("org\\.eclipse\\..+")) {
                callsModelIndex.setResolved(packageRoot, new LibraryIdentifier("org.eclipse", Version.create(3, 6)));
                return;
            }
        }

        callsModelIndex.setResolved(packageRoot, LibraryIdentifier.UNKNOWN);
    }

}
