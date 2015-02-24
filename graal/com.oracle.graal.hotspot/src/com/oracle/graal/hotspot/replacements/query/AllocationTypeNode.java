package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public class AllocationTypeNode extends ICGMacroNode implements CompilerDecisionQuery {

    public AllocationTypeNode(Invoke invoke) {
        super(invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        if (instrumentation.target() instanceof AbstractMergeNode) {
            AbstractMergeNode merge = (AbstractMergeNode) instrumentation.target();
            graph().replaceFixedWithFloating(this, graph().addWithoutUnique(CompilerDecisionUtil.createValuePhi(merge)));
        } else {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(-1, graph()));
        }
    }

}
