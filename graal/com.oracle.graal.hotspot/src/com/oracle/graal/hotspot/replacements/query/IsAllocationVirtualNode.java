package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public class IsAllocationVirtualNode extends ICGMacroNode implements CompilerDecisionQuery {

    public IsAllocationVirtualNode(Invoke invoke) {
        super(invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        ValueNode target = instrumentation.target();
        boolean isAllocationVirtual = target == null || target.isDeleted();
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(isAllocationVirtual, graph()));
    }

}
