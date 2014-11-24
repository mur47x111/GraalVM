/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.nodes.calc;

import com.oracle.graal.compiler.common.calc.*;
import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.spi.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.util.*;

@NodeInfo(shortName = "==")
public class PointerEqualsNode extends CompareNode {

    /**
     * Constructs a new pointer equality comparison node.
     *
     * @param x the instruction producing the first input to the instruction
     * @param y the instruction that produces the second input to this instruction
     */
    public static PointerEqualsNode create(ValueNode x, ValueNode y) {
        return new PointerEqualsNode(x, y);
    }

    protected PointerEqualsNode(ValueNode x, ValueNode y) {
        super(x, y);
        assert x.stamp() instanceof AbstractPointerStamp;
        assert y.stamp() instanceof AbstractPointerStamp;
    }

    @Override
    public Condition condition() {
        return Condition.EQ;
    }

    @Override
    public boolean unorderedIsTrue() {
        return false;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY) {
        if (GraphUtil.unproxify(forX) == GraphUtil.unproxify(forY)) {
            return LogicConstantNode.tautology();
        } else if (forX.stamp().alwaysDistinct(forY.stamp())) {
            return LogicConstantNode.contradiction();
        } else if (((AbstractPointerStamp) forX.stamp()).alwaysNull()) {
            return IsNullNode.create(forY);
        } else if (((AbstractPointerStamp) forY.stamp()).alwaysNull()) {
            return IsNullNode.create(forX);
        }
        return super.canonical(tool, forX, forY);
    }

    @Override
    protected CompareNode duplicateModified(ValueNode newX, ValueNode newY) {
        return PointerEqualsNode.create(newX, newY);
    }
}
