/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olav Lenz - initial API and implementation
 */
package org.eclipse.recommenders.models;

import static com.google.common.base.Preconditions.checkArgument;
import static org.eclipse.recommenders.utils.Checks.ensureIsNotNull;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class DependencyInfo {

    public static final String EXECUTION_ENVIRONMENT = "EXECUTION_ENVIRONMENT";
    public static final String EXECUTION_ENVIRONMENT_VERSION = "EXECUTION_ENVIRONMENT_VERSION";

    private final File file;
    private final DependencyType type;
    private final Map<String, String> hints;

    public DependencyInfo(File file, DependencyType type) {
        this(file, type, Collections.<String, String>emptyMap());
    }

    public DependencyInfo(File file, DependencyType type, Map<String, String> hint) {
        this.file = ensureIsNotNull(file);
        checkArgument(file.isAbsolute());
        this.type = type;
        this.hints = ensureIsNotNull(hint);
    }

    public File getFile() {
        return file;
    }

    public DependencyType getType() {
        return type;
    }

    public Optional<String> getHint(String key) {
        return Optional.fromNullable(hints.get(key));
    }

    public Map<String, String> getHintMap() {
        return ImmutableMap.copyOf(hints);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(file).append(type).append(hints).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DependencyInfo) {
            final DependencyInfo other = (DependencyInfo) obj;
            return new EqualsBuilder().append(file, other.file).append(type, other.type)
                    .append(hints, other.hints).isEquals();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper("").addValue(file).addValue(type).addValue(hints).toString();
    }
}
