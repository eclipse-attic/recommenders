/**
 * Copyright (c) 2011 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Michael Kutschke - initial API and implementation.
 */
package org.eclipse.recommenders.tests.jayes;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import org.eclipse.recommenders.jayes.factor.AbstractFactor;
import org.eclipse.recommenders.jayes.factor.DenseFactor;
import org.eclipse.recommenders.jayes.factor.SparseFactor;
import org.eclipse.recommenders.jayes.factor.arraywrapper.DoubleArrayWrapper;
import org.eclipse.recommenders.jayes.util.MathUtils;
import org.eclipse.recommenders.tests.jayes.util.ArrayUtils;
import org.junit.Test;

public class FactorTest {

    private static double[] distribution2x2x2() {
        // @formatter:off
        return ArrayUtils.flatten(new double[][][] { { { 0.5, 0.5 }, { 1.0, 0.0 } }, { { 0.4, 0.6 }, { 0.3, 0.7 } } });
        // @formatter:on

    }

    private static final double TOLERANCE = 0.00001;

    @Test
    public void testSum() {
        AbstractFactor factor = new DenseFactor();
        factor.setDimensionIDs(0, 1);
        factor.setDimensions(2, 2);
        factor.setValues(new DoubleArrayWrapper(0.5, 0.5, 1.0, 0.0));
        double[] prob = MathUtils.normalize(factor.marginalizeAllBut(-1));
        assertArrayEquals(prob, new double[] { 0.75, 0.25 }, TOLERANCE);
    }

    @Test
    public void testSelectAndSum() {
        AbstractFactor factor = create2x2x2Factor();
        factor.setValues(new DoubleArrayWrapper(distribution2x2x2()));
        factor.select(0, 0);
        double[] prob = MathUtils.normalize(factor.marginalizeAllBut(-1));
        assertArrayEquals(prob, new double[] { 0.75, 0.25 }, TOLERANCE);

        factor.select(0, -1);
        factor.select(1, 1);
        prob = MathUtils.normalize(factor.marginalizeAllBut(-1));
        assertArrayEquals(prob, new double[] { 0.65, 0.35 }, TOLERANCE);
    }

    private AbstractFactor create2x2x2Factor() {
        AbstractFactor factor = new DenseFactor();
        factor.setDimensionIDs(0, 1, 2);
        factor.setDimensions(2, 2, 2);
        return factor;
    }

    @Test
    public void testSumMiddle1() {
        AbstractFactor factor = create2x2x2Factor();
        factor.setValues(new DoubleArrayWrapper(distribution2x2x2()));
        factor.select(2, 0);
        double[] prob = MathUtils.normalize(factor.marginalizeAllBut(1));
        assertArrayEquals(prob, new double[] { 0.9 / 2.2, 1.3 / 2.2 }, TOLERANCE);
    }

    @Test
    public void testSumMiddle2() {
        AbstractFactor factor = create2x2x2Factor();
        factor.setValues(new DoubleArrayWrapper(distribution2x2x2()));
        factor.select(0, 1);
        factor.select(2, 1);
        double[] prob = MathUtils.normalize(factor.marginalizeAllBut(1));
        assertArrayEquals(prob, new double[] { 0.6 / 1.3, 0.7 / 1.3 }, TOLERANCE);
    }

    @Test
    public void testMultiplication() {
        AbstractFactor f1 = create2x2x2Factor();
        // @formatter:off
        f1.setValues(new DoubleArrayWrapper(ArrayUtils.flatten(new double[][][] { { { 0.5, 0.5 }, { 0.5, 0.5 } },
                { { 0.5, 0.5 }, { 0.5, 0.5 } } })));
        // @formatter:on

        AbstractFactor f2 = new DenseFactor();
        f2.setDimensionIDs(2, 0);
        f2.setDimensions(2, 2);
        f2.setValues(new DoubleArrayWrapper(1.0, 0.0, 0.0, 1.0));

        f1.multiplyCompatible(f2);
        assertArrayEquals(f1.getValues().toDoubleArray(), new double[] { 0.5, 0.0, 0.5, 0.0, 0.0, 0.5, 0.0, 0.5 },
                TOLERANCE);
    }

    @Test
    public void testPreparedSum() {
        AbstractFactor f = new DenseFactor();
        f.setDimensionIDs(0, 1, 2);
        f.setDimensions(4, 4, 4);
        f.fill(1);

        AbstractFactor f2 = new DenseFactor();
        f2.setDimensionIDs(2);
        f2.setDimensions(4);

        f.sumPrepared(f2.getValues(), f.prepareMultiplication(f2));

        assertArrayEquals(f.marginalizeAllBut(-1), f2.getValues().toDoubleArray(), TOLERANCE);
    }

    @Test
    public void testCopy() {
        AbstractFactor f = create2x2x2Factor();

        f.select(2, 1);

        // no ArrayIndexOutOfBoundsException should be thrown
        f.copyValues(new DoubleArrayWrapper(1, 1, 1, 1, 1, 1, 1, 1));

        for (int oddIndex = 1; oddIndex < f.getValues().length(); oddIndex += 2) {
            assertThat(f.getValue(oddIndex), is(1.0));
        }
    }

    @Test
    public void testMultiplySparseFactor() {
        AbstractFactor f = create2x2x2Factor();
        f.setValues(new DoubleArrayWrapper(distribution2x2x2()));

        AbstractFactor f2 = SparseFactor.fromFactor(f);
        f.multiplyCompatible(f2);

        // @formatter:off
        assertArrayEquals(
                f.getValues().toDoubleArray(),
                ArrayUtils.flatten(new double[][][] { { { 0.5 * 0.5, 0.5 * 0.5 }, { 1.0, 0.0 } },
                        { { 0.4 * 0.4, 0.6 * 0.6 }, { 0.3 * 0.3, 0.7 * 0.7 } } }), TOLERANCE);
        // @formatter:on
    }

    @Test
    public void testZeroDimensional() {
        AbstractFactor dense = new DenseFactor();
        dense.setDimensionIDs();
        dense.setDimensions();
        dense.setValues(new DoubleArrayWrapper(2));

        AbstractFactor dense2 = new DenseFactor();
        dense2.setDimensionIDs();
        dense2.setDimensions();
        dense2.setValues(new DoubleArrayWrapper(3));

        dense2.multiplyCompatible(dense);

        assertThat(dense2.getValues().toDoubleArray(), is(new double[] { 6 }));
    }

}
