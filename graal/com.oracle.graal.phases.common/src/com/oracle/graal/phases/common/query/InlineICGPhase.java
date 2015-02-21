package com.oracle.graal.phases.common.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.tiers.*;

public class InlineICGPhase extends BasePhase<LowTierContext> {

    @Override
    protected void run(StructuredGraph graph, LowTierContext context) {
        for (Node node : graph.getNodes()) {
            if (node instanceof InstrumentationNode) {
                ((InstrumentationNode) node).inline();
            }
        }
    }

}