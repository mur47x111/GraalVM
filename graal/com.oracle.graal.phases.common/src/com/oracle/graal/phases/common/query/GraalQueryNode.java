package com.oracle.graal.phases.common.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;

@NodeInfo
public abstract class GraalQueryNode extends FixedWithNextNode {

    public static final NodeClass<GraalQueryNode> TYPE = NodeClass.create(GraalQueryNode.class);

    public GraalQueryNode(NodeClass<? extends FixedWithNextNode> c, Stamp stamp) {
        super(c, stamp);
    }

    protected void onExtractICG(@SuppressWarnings("unused") InstrumentationNode instrumentation) {
    }

    protected void onInlineICG(@SuppressWarnings("unused") InstrumentationNode instrumentation, @SuppressWarnings("unused") FixedNode position) {
    }

}
