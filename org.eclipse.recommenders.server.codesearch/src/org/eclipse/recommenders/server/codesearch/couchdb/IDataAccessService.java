/**
 * Copyright (c) 2011 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.server.codesearch.couchdb;

import org.eclipse.recommenders.commons.codesearch.SnippetSummary;

public interface IDataAccessService {

    public void saveSnippet(SnippetSummary type);

    public SnippetSummary getSnippet(String codeSnippetId);

}
