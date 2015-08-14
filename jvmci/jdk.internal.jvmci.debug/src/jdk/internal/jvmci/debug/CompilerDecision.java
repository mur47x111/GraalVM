package jdk.internal.jvmci.debug;

/**
 * NOTE that these queries return fixed constants in the interpreter mode. The Graal option
 * RemoveNeverExecutedCode is switched off to prevent de-optimization.
 *
 * @author zhengy
 *
 */
public final class CompilerDecision {

    // Static query intrinsics

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
     * @return the name of the root method for the current compilation task. If the enclosing method
     *         is inlined, this query returns the name of the method into which it is inlined.
     */
    public static String getRootName() {
        return "unknown";
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