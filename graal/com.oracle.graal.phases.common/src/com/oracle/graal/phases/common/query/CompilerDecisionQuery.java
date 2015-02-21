package com.oracle.graal.phases.common.query;

import com.oracle.graal.nodes.*;

public interface CompilerDecisionQuery {

    default ConstantNode resolve() {
        return null;
    }

    default void inline(@SuppressWarnings("unused") InstrumentationNode instrumentation) {
    }
}
