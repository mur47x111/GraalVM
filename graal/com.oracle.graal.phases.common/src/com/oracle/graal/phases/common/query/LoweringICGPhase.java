package com.oracle.graal.phases.common.query;

import com.oracle.graal.debug.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.query.*;
import com.oracle.graal.phases.tiers.*;

public class LoweringICGPhase extends BasePhase<PhaseContext> {

    private final CanonicalizerPhase canonicalizer;
    private final LoweringTool.LoweringStage loweringStage;

    public LoweringICGPhase(CanonicalizerPhase canonicalizer, LoweringTool.LoweringStage loweringStage) {
        this.canonicalizer = canonicalizer;
        this.loweringStage = loweringStage;
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context) {

        for (Node node : graph.getNodes()) {
            if (node instanceof InstrumentationNode) {
                InstrumentationNode instrumentation = (InstrumentationNode) node;
                InsertedCodeGraph icg = instrumentation.getICG();

                if (!icg.lowered(loweringStage)) {
                    new LoweringPhase(canonicalizer, loweringStage).apply(icg.graph(), context, false);
                    icg.addLoweringStage(loweringStage);
                    Debug.dump(icg, "After lowering ICG at " + loweringStage);
                }
            }
        }
    }

}