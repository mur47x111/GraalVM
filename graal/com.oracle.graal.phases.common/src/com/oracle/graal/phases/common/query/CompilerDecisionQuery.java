package com.oracle.graal.phases.common.query;

public interface CompilerDecisionQuery {

    default void onExtractICG(@SuppressWarnings("unused") InstrumentationNode instrumentation) {
    }

    default void onInlineICG(@SuppressWarnings("unused") InstrumentationNode instrumentation) {
    }

}
