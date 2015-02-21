package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.common.query.*;
import com.oracle.graal.phases.common.query.ExtractICGPhase.ICGBoundary;
import com.oracle.graal.replacements.nodes.*;

@NodeInfo
public class InstrumentationEndNode extends MacroNode implements CompilerDecisionQuery, ICGBoundary {

    public InstrumentationEndNode(Invoke invoke) {
        super(invoke);
    }

}