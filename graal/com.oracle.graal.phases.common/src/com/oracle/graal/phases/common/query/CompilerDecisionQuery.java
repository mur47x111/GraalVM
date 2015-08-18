package com.oracle.graal.phases.common.query;

import com.oracle.graal.nodes.*;

public interface CompilerDecisionQuery {

    default void onExtractICG(@SuppressWarnings("unused") InstrumentationNode instrumentation) {
    }

    default void onInlineICG(@SuppressWarnings("unused") InstrumentationNode instrumentation, @SuppressWarnings("unused") FixedNode position) {
    }

}
