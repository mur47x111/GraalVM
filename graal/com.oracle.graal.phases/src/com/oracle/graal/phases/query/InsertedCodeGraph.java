package com.oracle.graal.phases.query;

import java.util.*;

import com.oracle.graal.nodes.*;

public class InsertedCodeGraph {

    private final StructuredGraph graph;

    private ArrayList<String> passed;

    public InsertedCodeGraph(StructuredGraph graph) {
        this.graph = graph;
        this.passed = new ArrayList<>(16);
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

}
