package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public class AllocationTypeNode extends ICGMacroNode implements CompilerDecisionQuery {

    public AllocationTypeNode(Invoke invoke) {
        super(invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        ValueNode value = GraphUtil.unproxify(instrumentation.target());

        if (value instanceof ValuePhiNode) {
            ValuePhiNode phi = (ValuePhiNode) value;
            AbstractMergeNode merge = phi.merge();
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
