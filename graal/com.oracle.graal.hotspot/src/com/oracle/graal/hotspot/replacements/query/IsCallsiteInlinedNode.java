package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public class IsCallsiteInlinedNode extends ICGMacroNode implements CompilerDecisionQuery {

    public IsCallsiteInlinedNode(Invoke invoke) {
        super(invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        FixedNode target = instrumentation.target();
        boolean isCallsiteInlined = target == null || target.isDeleted();
        graph().replaceFixedWithFloating(this, ConstantNode.forBoolean(isCallsiteInlined, graph()));
    }

}
