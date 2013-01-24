package org.eclipse.recommenders.webclient.results;

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

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GenericResultObjectView<T> {

    public int total_rows;
    public int offset;
    public List<ResultObject<T>> rows;

    public List<T> getTransformedResult() {
        final List<T> result = new LinkedList<T>();
        for (final ResultObject<T> obj : rows) {
            result.add(obj.value);
        }
        return result;
    }
}
