package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public class LockTypeNode extends ICGMacroNode implements CompilerDecisionQuery {

    public LockTypeNode(Invoke invoke) {
        super(invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        if (instrumentation.target() instanceof AbstractMergeNode) {
            AbstractMergeNode merge = (AbstractMergeNode) instrumentation.target();
            ValuePhiNode path = graph().addWithoutUnique(new ValuePhiNode(stamp(), merge));

            for (int i = 0; i < merge.cfgPredecessors().count(); i++) {
                path.addInput(ConstantNode.forInt(i, graph()));
            }

            graph().replaceFixedWithFloating(this, path);
        } else {
            graph().replaceFixedWithFloating(this, ConstantNode.forInt(-1, graph()));
        }
    }

}
