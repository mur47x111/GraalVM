package com.oracle.graal.debug.query;

/**
 * NOTE that these queries return fixed constants in the interpreter mode. The Graal option
 * RemoveNeverExecutedCode is switched off to prevent de-optimization.
 *
 * @author zhengy
 *
 */
public final class GraalQueryAPI {

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

    public final static int ERROR = -1;

    /**
     * @return the kind of heap allocation for a directly preceding allocation site. The possible
     *         return values are {ERROR(-1), TLAB(0), HEAP(1)}. While ERROR denotes either the
     *         utility is not supported, e.g. in interpreter, or if the allocation site was
     *         eliminated, the other two represent a TLAB allocation (fast path) or a direct heap
     *         allocation (slow path).
     */
    public static int getAllocationType() {
        return ERROR;
    }

    /**
     * @return the runtime lock type for a directly preceding lock site. The possible return values
     *         are {ERROR(-1), bias:existing(0), bias:acquired(1), bias:transfer(2),
     *         stub:revoke_or_stub:epoch-expired(3), stub:failed-cas(4), recursive(5), cas(6)}.
     */
    public static int getLockType() {
        return ERROR;
    }

    public static int getDeoptReason() {
        return ERROR;
    }

    public static int getDeoptAction() {
        return ERROR;
    }

    public static int getDeoptBCI() {
        return ERROR;
    }

}