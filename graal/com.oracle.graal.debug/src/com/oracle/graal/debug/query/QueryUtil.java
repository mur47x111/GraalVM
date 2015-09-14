package com.oracle.graal.debug.query;

import jdk.internal.jvmci.meta.*;

public class QueryUtil {

    public static final String CN_DELIMITATIONAPI = DelimitationAPI.class.getName();
    public static final String CN_GRAALQUERYAPI = GraalQueryAPI.class.getName();

    public static boolean isQueryIntrinsic(ResolvedJavaMethod method) {
        String klass = method.getDeclaringClass().toJavaName();
        return CN_DELIMITATIONAPI.equals(klass) || CN_GRAALQUERYAPI.equals(klass);
    }

}
