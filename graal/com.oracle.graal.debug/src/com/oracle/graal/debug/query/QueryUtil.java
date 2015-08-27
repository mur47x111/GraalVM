package com.oracle.graal.debug.query;

import jdk.internal.jvmci.meta.*;

public class QueryUtil {

    public static final String PREFIX = QueryUtil.class.getPackage().getName();

    public static boolean isQueryIntrinsic(ResolvedJavaMethod method) {
        return method.getDeclaringClass().toJavaName().startsWith(PREFIX);
    }

}
