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
package org.eclipse.recommenders.rcp

import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.dom.ITypeBinding
import org.eclipse.jdt.core.dom.IVariableBinding
import org.eclipse.jdt.core.dom.Name
import org.eclipse.jdt.core.dom.ParameterizedType
import org.eclipse.jdt.core.dom.SimpleName
import org.eclipse.jdt.core.dom.TypeParameter
import org.eclipse.recommenders.rcp.utils.AstBindings
import org.eclipse.recommenders.testing.jdt.JavaProjectFixture
import org.junit.Test
import org.mockito.Mockito

import static org.eclipse.jdt.core.dom.NodeFinder.*
import static org.eclipse.recommenders.testing.CodeBuilder.*
import static org.junit.Assert.*

class AstBindingsResolverTest {

    JavaElementResolver jdtResolver = new JavaElementResolver()

    @Test
    def void test01() {
        val names = newArrayList("String", "Integer", "List", "List<Integer>", "List<? extends Number>")
        for (name : names) {
            val code = method('''«name»$ var;''')
            val binding = findTypeBinding(code)
            val recTypeName = AstBindings::toTypeName(binding).get
            assertTrue(jdtResolver.toJdtType(recTypeName).present);
        }
    }

    @Test
    def void testGetVariableBinding_var() {
        val expected = Mockito.mock(IVariableBinding)
        val name = Mockito.mock(Name)
        Mockito.when(name.resolveBinding).thenReturn(expected)
        assertEquals(expected, AstBindings.getVariableBinding(name).orNull)
    }

    @Test
    def void testGetVariableBinding_type() {
        val expected = Mockito.mock(ITypeBinding)
        val name = Mockito.mock(Name)
        Mockito.when(name.resolveBinding).thenReturn(expected)
        assertNull(AstBindings.getVariableBinding(name).orNull)
    }

    @Test
    def void testGetVariableBinding_null() {
        val name = Mockito.mock(Name)
        Mockito.when(name.resolveBinding).thenReturn(null)
        assertNull(AstBindings.getVariableBinding(name).orNull)
    }

    def ITypeBinding findTypeBinding(CharSequence code) {
        val fixture = new JavaProjectFixture(ResourcesPlugin::getWorkspace(), "test")
        val struct = fixture.parseWithMarkers(code.toString)
        val cu = struct.first;
        val pos = struct.second.head;
        val selection = perform(cu, pos, 0);
        switch (selection) {
            SimpleName: {
                return selection.resolveBinding as ITypeBinding
            }
            TypeParameter: {
                return selection.resolveBinding

            }
            ParameterizedType: {
                return selection.resolveBinding
            }
        }
        throw new IllegalArgumentException()
    }
}
