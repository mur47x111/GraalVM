package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public class ObservedReferenceNode extends ICGMacroNode implements CompilerDecisionQuery {

    public ObservedReferenceNode(Invoke invoke) {
        super(invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        GraphUtil.unlinkFixedNode(this);
        replaceAtUsages(inputs().first());
        safeDelete();
    }

}