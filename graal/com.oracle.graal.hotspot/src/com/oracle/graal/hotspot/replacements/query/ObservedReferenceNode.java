package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.util.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.query.*;

@NodeInfo
public final class ObservedReferenceNode extends ICGMacroNode implements CompilerDecisionQuery {

    public static final NodeClass<ObservedReferenceNode> TYPE = NodeClass.create(ObservedReferenceNode.class);

    public ObservedReferenceNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public void inline(InstrumentationNode instrumentation) {
        GraphUtil.unlinkFixedNode(this);
        replaceAtUsages(inputs().first());
        safeDelete();
    }

}