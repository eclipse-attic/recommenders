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
package org.eclipse.recommenders.jayes.lbp;

import static org.junit.Assert.assertArrayEquals;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferrer;
import org.eclipse.recommenders.testing.jayes.NetExamples;
import org.junit.Test;

public class LoopyBeliefPropagationTest {

    @Test
    public void testSampler1() {
        BayesNet net = NetExamples.testNet1();
        BayesNode a = net.getNode("a");
        BayesNode b = net.getNode("b");
        BayesNode c = net.getNode("c");

        IBayesInferrer sampler = new LoopyBeliefPropagation();
        sampler.setNetwork(net);
        sampler.addEvidence(a, "false");
        sampler.addEvidence(b, "lu");

        assertArrayEquals(sampler.getBeliefs(c), new double[] { 0.7, 0.3 }, 0.01);
    }

}
