package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class IsAllocationVirtualNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<IsAllocationVirtualNode> TYPE = NodeClass.create(IsAllocationVirtualNode.class);

    public IsAllocationVirtualNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        ValueNode target = instrumentation.target();
        boolean isAllocationVirtual = target == null || target.isDeleted();
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(isAllocationVirtual, graph()));
    }

    public ConstantNode defaultValue() {
        return ConstantNode.forBoolean(false, graph());
    }

}
