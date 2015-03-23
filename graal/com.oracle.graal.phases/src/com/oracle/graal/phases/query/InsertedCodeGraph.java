package com.oracle.graal.phases.query;

import java.util.*;

import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

public class InsertedCodeGraph {

    private final StructuredGraph graph;

    private ArrayList<String> passed;
    private ArrayList<LoweringTool.LoweringStage> loweringStages;

    public InsertedCodeGraph(StructuredGraph graph) {
        this.graph = graph;
        this.passed = new ArrayList<>(16);
        this.loweringStages = new ArrayList<>(4);
    }

    public boolean isPassed(String stage) {
        return passed.contains(stage);
    }

    public void pass(String stage) {
        passed.add(stage);
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
