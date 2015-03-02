package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class LockTypeNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<LockTypeNode> TYPE = NodeClass.create(LockTypeNode.class);

    public LockTypeNode(Invoke invoke) {
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
