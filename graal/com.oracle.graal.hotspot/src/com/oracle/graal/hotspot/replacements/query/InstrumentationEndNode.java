package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.ICGBoundaryEnd;

@NodeInfo
public class InstrumentationEndNode extends ICGMacroNode implements CompilerDecisionQuery, ICGBoundaryEnd {

    public InstrumentationEndNode(Invoke invoke) {
        super(invoke);
    }

    public FixedWithNextNode asFixedWithNextNode() {
        return this;
    }

}