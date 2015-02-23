package com.oracle.graal.phases.query;

import java.util.*;

import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

public class InsertedCodeGraph {

    private final StructuredGraph graph;
    private boolean passGuardLowering = false;
    private boolean passFrameStateAssignent = false;
    private ArrayList<LoweringTool.LoweringStage> loweringStages;

    public InsertedCodeGraph(StructuredGraph graph) {
        this.graph = graph;
        this.loweringStages = new ArrayList<>(4);
    }

    public boolean passGuardLowering() {
        return passGuardLowering;
    }

    public boolean passFrameStateAssignent() {
        return passFrameStateAssignent;
    }

    public void setPassGuardLowering() {
        passGuardLowering = true;
    }

    public void setPassFrameStateAssignent() {
        passFrameStateAssignent = true;
    }

    public StructuredGraph graph() {
        return graph;
    }

    public boolean lowered(LoweringTool.LoweringStage stage) {
        return loweringStages.contains(stage);
    }

    public void addLoweringStage(LoweringTool.LoweringStage stage) {
        loweringStages.add(stage);
    }

}
