package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;

@NodeInfo
public class RootNameNode extends ICGMacroNode implements CompilerDecisionQuery {

    public RootNameNode(Invoke invoke) {
        super(invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        graph().replaceFixedWithFloating(this, graph().unique(CompilerDecisionUtil.getMethodFullName(instrumentation.graph().method())));
    }

}
