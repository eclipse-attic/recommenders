/**
 * Copyright (c) 2010, 2014 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.stacktraces;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.recommenders.utils.Nullable;

import com.google.common.base.Joiner;

public class ThrowableDto {

    public static @Nullable ThrowableDto from(@Nullable Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        ThrowableDto res = new ThrowableDto();
        res.message = throwable.getMessage();
        res.classname = throwable.getClass().getName();

        res.elements = new LinkedList<StackTraceElementDto>();
        for (StackTraceElement el : throwable.getStackTrace()) {
            res.elements.add(StackTraceElementDto.from(el));
        }
        if (throwable instanceof Exception) {
            res.cause = from(throwable.getCause());
        }
        return res;
    }

    public String classname;
    public String message;
    public List<StackTraceElementDto> elements;
    public ThrowableDto cause;

    @Override
    public String toString() {
        String s = classname + ": " + message + Joiner.on("\n\t").join(elements);
        if (cause != null) {
            s += "\ncause:\n" + cause.toString();
        }
        return s;
    }
}
