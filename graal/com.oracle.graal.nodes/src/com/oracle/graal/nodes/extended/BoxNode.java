/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.extended;

import java.util.*;

import jdk.internal.jvmci.meta.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.graph.spi.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.java.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.nodes.type.*;
import com.oracle.graal.nodes.virtual.*;

/**
 * This node represents the boxing of a primitive value. This corresponds to a call to the valueOf
 * methods in Integer, Long, etc.
 */
@NodeInfo
public final class BoxNode extends FixedWithNextNode implements VirtualizableAllocation, Lowerable, Canonicalizable.Unary<ValueNode> {

    public static final NodeClass<BoxNode> TYPE = NodeClass.create(BoxNode.class);
    @Input private ValueNode value;
    private final Kind boxingKind;

    public BoxNode(ValueNode value, ResolvedJavaType resultType, Kind boxingKind) {
        super(TYPE, StampFactory.exactNonNull(resultType));
        this.value = value;
        this.boxingKind = boxingKind;
    }

    public Kind getBoxingKind() {
        return boxingKind;
    }

    public ValueNode getValue() {
        return value;
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue) {
        if (tool.allUsagesAvailable() && hasNoUsages()) {
            return null;
        }
        return this;
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        ValueNode v = tool.getReplacedValue(getValue());
        ResolvedJavaType type = StampTool.typeOrNull(stamp());

        VirtualBoxingNode newVirtual = new VirtualBoxingNode(type, boxingKind);
        assert newVirtual.getFields().length == 1;

        tool.createVirtualObject(newVirtual, new ValueNode[]{v}, Collections.<MonitorIdNode> emptyList(), false);
        tool.replaceWithVirtual(newVirtual);
    }
}
