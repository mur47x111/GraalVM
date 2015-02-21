package com.oracle.graal.debug.external;

/**
 * NOTE that the APIs from this interface return fixed constants in the interpreter mode. Consider
 * add option -G:-RemoveNeverExecutedCode to prevent a single deoptimisation for the compiled code.
 * Also since inlining decision are based on profiling, callsites affected by these apis might not
 * be inlined.
 *
 * @author zhengy
 *
 */
@SuppressWarnings("unused")
public final class CompilerDecision {

    private CompilerDecision() {
    }

    public static void instrumentationBegin(boolean target) {
    }

    public static void instrumentationEnd() {
    }

    /**
     * @return true when compiled
     */
    public static boolean isMethodCompiled() {
        return false;
    }

    /**
     * @return the method name after InliningPhase
     */
    public static String getMethodName() {
        return "UNKNOWN";
    }

}