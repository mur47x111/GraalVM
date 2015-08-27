package com.oracle.graal.debug.query;

public class DelimitationAPI {

    /**
     * Marks the beginning of the instrumentation boundary. - The target parameter indicates whether
     * to associate the instrumentation with the preceding or the following base program IR node.
     * Supported values are -1 (predecessor), 1 (successor)}.
     */
    public static void instrumentationBegin(@SuppressWarnings("unused") int offset) {
    }

    public static void instrumentationBegin(@SuppressWarnings("unused") int offset, @SuppressWarnings("unused") int type) {
    }

    /**
     * Marks the end of the instrumentation boundary.
     */
    public static void instrumentationEnd() {
    }

}
