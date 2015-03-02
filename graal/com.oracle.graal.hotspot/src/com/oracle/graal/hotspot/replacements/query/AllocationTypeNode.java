package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class AllocationTypeNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<AllocationTypeNode> TYPE = NodeClass.create(AllocationTypeNode.class);

    public AllocationTypeNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        if (instrumentation.target() instanceof AbstractMergeNode) {
            AbstractMergeNode merge = (AbstractMergeNode) instrumentation.target();
            graph().replaceFixedWithFloating(this, CompilerDecisionUtil.createValuePhi(graph(), merge));
        } else {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(-1, graph()));
        }
    }

}
