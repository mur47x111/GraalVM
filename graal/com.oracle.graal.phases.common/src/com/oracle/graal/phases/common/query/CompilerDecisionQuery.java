package com.oracle.graal.phases.common.query;

import com.oracle.graal.nodes.*;
import com.oracle.graal.phases.query.*;

public interface CompilerDecisionQuery {

    default ConstantNode resolve() {
        return null;
    }

    default void inline(@SuppressWarnings("unused") InstrumentationNode instrumentation) {
    }

}
