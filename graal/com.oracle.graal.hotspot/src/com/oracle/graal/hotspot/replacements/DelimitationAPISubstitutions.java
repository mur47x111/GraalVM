package com.oracle.graal.hotspot.replacements;

import jdk.internal.jvmci.debug.*;

import com.oracle.graal.api.replacements.*;
import com.oracle.graal.phases.common.query.*;

@ClassSubstitution(DelimitationAPI.class)
public class DelimitationAPISubstitutions {

    @MethodSubstitution(isStatic = true)
    public static void instrumentationBegin(int target) {
        InstrumentationBeginNode.instantiate(target, 0);
    }

    @MethodSubstitution(isStatic = true)
    public static void instrumentationBegin(int target, int type) {
        InstrumentationBeginNode.instantiate(target, type);
    }

    @MethodSubstitution(isStatic = true)
    public static void instrumentationEnd() {
        InstrumentationEndNode.instantiate();
    }

}
