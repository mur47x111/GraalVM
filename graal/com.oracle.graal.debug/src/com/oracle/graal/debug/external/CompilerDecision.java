package com.oracle.graal.debug.external;

/**
 * NOTE that these queries return fixed constants in the interpreter mode. The Graal option
 * RemoveNeverExecutedCode is switched off to prevent de-optimization.
 *
 * @author zhengy
 *
 */
@SuppressWarnings("unused")
public final class CompilerDecision {

    private CompilerDecision() {
    }

    public final static String UNKNOWN = "UNKNOWN";

    // Compiler and developer hints

    /**
     * Marks the beginning of the instrumentation boundary. - The target parameter indicates whether
     * to associate the instrumentation with the preceding or the following base program IR node.
     * Supported values are -1 (predecessor), 1 (successor)}.
     */
    public static void instrumentationBegin(int target) {
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
        return UNKNOWN;
    }

    /**
     * @return the name of the root method for the current compilation task. If the enclosing method
     *         is inlined, this query returns the name of the method into which it is inlined.
     */
    public static String getRootName() {
        return UNKNOWN;
    }

    // Dynamic query intrinsics

    public final static int ERROR = 0;

    /**
     * @return the kind of heap allocation for a directly preceding allocation site. The possible
     *         return values are {ERROR(0), TLAB(1), HEAP(2)}. While ERROR denotes either the
     *         utility is not supported, e.g. in interpreter, or if the allocation site was
     *         eliminated, the other two represent a TLAB allocation (fast path) or a direct heap
     *         allocation (slow path).
     */
    public static int getAllocationType() {
        return ERROR;
    }

    /**
     * @return the runtime lock type for a directly preceding lock site. The possible return values
     *         are {ERROR(0), STUB_REVOKE(1), STUB_EPOCH_EXPIRED(2), STUB_FAILED-CAS(3),
     *         BIAS_EXISTING(4), BIAS_ACQUIRED(5), BIAS_TRANSFER(6), RECURSIVE(7), CAS(8)}.
     */
    public static int getLockType() {
        return ERROR;
    }

}