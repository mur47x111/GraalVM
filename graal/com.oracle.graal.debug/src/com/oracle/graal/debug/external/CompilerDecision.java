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

    // Compiler and developer hints

    /**
     * Marks the beginning of the instrumentation boundary. - The target parameter indicates whether
     * to associate the instrumentation with the preceding or the following base program IR node.
     * Supported values are false(predecessor), true (successor)}.
     */
    public static void instrumentationBegin(boolean target) {
    }

    /**
     * Marks the end of the instrumentation boundary.
     */
    public static void instrumentationEnd() {
    }

    /**
     * Returns the object reference passed to the method as an argument, if it exists on the heap.
     * Otherwise returns null. Allows depending on a reference in a weak fashion, without forcing
     * allocation of the underlying object. The method is primarily meant to express the fact that a
     * reference that can be expected to exist in the given context can be null due to compiler
     * optimizations.
     */
    public static Object observedReference(Object object) {
        return object;
    }

    // Static query intrinsics

    /**
     * @return true if an allocation is virtual, i.e., replaced by a stack allocation. If an
     *         allocation site is cloned, the corresponding ICG is cloned as well, and the result of
     *         the query refers to the cloned allocation.
     */
    public static boolean isAllocationVirtual() {
        return false;
    }

    /**
     * @return true if the enclosing method has been compiled by the dynamic compiler.
     */
    public static boolean isMethodCompiled() {
        return false;
    }

    /**
     * @return true if the enclosing method is inlined.
     */
    public static boolean isMethodInlined() {
        return false;
    }

    /**
     * @return true if a call site is inlined.
     */
    public static boolean isCallsiteInlined() {
        return false;
    }

    /**
     * @return the name of the enclosing method.
     */
    public static String getMethodName() {
        return "UNKNOWN";
    }

    /**
     * @return the name of the root method for the current compilation task. If the enclosing method
     *         is inlined, this returns the name of the method into which it was inlined.
     */
    public static String getRootName() {
        return "UNKNOWN";
    }

    // Dynamic query intrinsics

    /**
     * @return the kind of heap allocation for a directly preceding allocation site. The possible
     *         return values are {HEAP, TLAB}, representing a direct heap allocation (slow path), or
     *         a TLAB allocation (fast path). If the allocation site was eliminated, the method
     *         returns a special error value.
     */
    public static int getAllocationType() {
        return 0;
    }

    /**
     * @return the runtime lock type for a directly preceding lock site. The possible return values
     *         are {BIASED, RECURSIVE, CAS, ...}, representing the different locking strategies.
     */
    public static int getLockType() {
        return 0;
    }

}