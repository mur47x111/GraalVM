package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.ICGBoundaryEnd;

@NodeInfo
public final class InstrumentationEndNode extends ICGMacroNode implements CompilerDecisionQuery, ICGBoundaryEnd {

    public static final NodeClass<InstrumentationEndNode> TYPE = NodeClass.create(InstrumentationEndNode.class);

    public InstrumentationEndNode(Invoke invoke) {
        super(TYPE, invoke);
    }

    public FixedWithNextNode asFixedWithNextNode() {
        return this;
    }

}