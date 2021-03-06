/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.jvmci.meta.*;
import jdk.internal.jvmci.meta.Assumptions.*;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.graph.spi.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

/**
 * Loads an object's hub. The object is not null-checked by this operation.
 */
@NodeInfo
public final class LoadHubNode extends FloatingGuardedNode implements Lowerable, Canonicalizable, Virtualizable {

    public static final NodeClass<LoadHubNode> TYPE = NodeClass.create(LoadHubNode.class);
    @Input ValueNode value;

    public ValueNode getValue() {
        return value;
    }

    private static Stamp hubStamp(StampProvider stampProvider, ValueNode value) {
        assert value.stamp() instanceof ObjectStamp;
        return stampProvider.createHubStamp(((ObjectStamp) value.stamp()));
    }

    public static ValueNode create(ValueNode value, StampProvider stampProvider, MetaAccessProvider metaAccess) {
        Stamp stamp = hubStamp(stampProvider, value);
        ValueNode synonym = findSynonym(value, stamp, null, metaAccess);
        if (synonym != null) {
            return synonym;
        }
        return new LoadHubNode(stamp, value, null);
    }

    public LoadHubNode(@InjectedNodeParameter StampProvider stampProvider, ValueNode value) {
        this(stampProvider, value, null);
    }

    public LoadHubNode(@InjectedNodeParameter StampProvider stampProvider, ValueNode value, ValueNode guard) {
        this(hubStamp(stampProvider, value), value, guard);
    }

    private LoadHubNode(Stamp stamp, ValueNode value, ValueNode guard) {
        super(TYPE, stamp, (GuardingNode) guard);
        assert value != guard;
        this.value = value;
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool) {
        MetaAccessProvider metaAccess = tool.getMetaAccess();
        ValueNode curValue = getValue();
        ValueNode newNode = findSynonym(curValue, stamp(), graph(), metaAccess);
        if (newNode != null) {
            return newNode;
        }
        return this;
    }

    public static ValueNode findSynonym(ValueNode curValue, Stamp stamp, StructuredGraph graph, MetaAccessProvider metaAccess) {
        ResolvedJavaType exactType = findSynonymType(graph, metaAccess, curValue);
        if (exactType != null) {
            return ConstantNode.forConstant(stamp, exactType.getObjectHub(), metaAccess);
        }
        return null;
    }

    public static ResolvedJavaType findSynonymType(StructuredGraph graph, MetaAccessProvider metaAccess, ValueNode curValue) {
        ResolvedJavaType exactType = null;
        if (metaAccess != null && curValue.stamp() instanceof ObjectStamp) {
            ObjectStamp objectStamp = (ObjectStamp) curValue.stamp();
            if (objectStamp.isExactType()) {
                exactType = objectStamp.type();
            } else if (objectStamp.type() != null && graph != null && graph.getAssumptions() != null) {
                AssumptionResult<ResolvedJavaType> leafConcreteSubtype = objectStamp.type().findLeafConcreteSubtype();
                if (leafConcreteSubtype != null) {
                    exactType = leafConcreteSubtype.getResult();
                    graph.getAssumptions().record(leafConcreteSubtype);
                }
            }
        }
        return exactType;
    }

    @Override
    public void virtualize(VirtualizerTool tool) {
        State state = tool.getObjectState(value);
        if (state != null) {
            Constant constantHub = state.getVirtualObject().type().getObjectHub();
            tool.replaceWithValue(ConstantNode.forConstant(stamp(), constantHub, tool.getMetaAccessProvider(), graph()));
        }
    }
}
