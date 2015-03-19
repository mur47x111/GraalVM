package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class IsCallsiteInlinedNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<IsCallsiteInlinedNode> TYPE = NodeClass.create(IsCallsiteInlinedNode.class);

    public IsCallsiteInlinedNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        ValueNode target = instrumentation.target();
        boolean isCallsiteInlined = target == null || target.isDeleted() || !(target instanceof Invoke);
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(isCallsiteInlined, graph()));
    }

    public ConstantNode defaultValue() {
        return ConstantNode.forBoolean(false, graph());
    }

}
