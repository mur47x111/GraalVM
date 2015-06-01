package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class IsHeapAllocNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<IsHeapAllocNode> TYPE = NodeClass.create(IsHeapAllocNode.class);

    public IsHeapAllocNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        ValueNode target = instrumentation.target();
        boolean isHeapAlloc = target != null && !target.isDeleted();
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(isHeapAlloc, graph()));
    }

    public ConstantNode defaultValue() {
        return ConstantNode.forBoolean(true, graph());
    }

}
