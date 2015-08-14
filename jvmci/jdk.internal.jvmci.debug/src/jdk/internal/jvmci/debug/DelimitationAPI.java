package jdk.internal.jvmci.debug;

public class DelimitationAPI {

    /**
     * Marks the beginning of the instrumentation boundary. - The target parameter indicates whether
     * to associate the instrumentation with the preceding or the following base program IR node.
     * Supported values are -1 (predecessor), 1 (successor)}.
     */
    public static void instrumentationBegin(@SuppressWarnings("unused") int offset) {
    }

    /**
     * Marks the end of the instrumentation boundary.
     */
    public static void instrumentationEnd() {
    }

}
