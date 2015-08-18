package com.oracle.graal.phases.common.query;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;

@NodeInfo
public abstract class CompilerDecisionQueryNode extends FixedWithNextNode {

    public static final NodeClass<CompilerDecisionQueryNode> TYPE = NodeClass.create(CompilerDecisionQueryNode.class);

    public CompilerDecisionQueryNode(NodeClass<? extends FixedWithNextNode> c, Stamp stamp) {
        super(c, stamp);
    }

    protected void onExtractICG(@SuppressWarnings("unused") InstrumentationNode instrumentation) {
    }

    protected void onInlineICG(@SuppressWarnings("unused") InstrumentationNode instrumentation, @SuppressWarnings("unused") FixedNode position) {
    }

    protected abstract void replaceWithDefault();

}
