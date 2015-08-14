package com.oracle.graal.hotspot.replacements;

import jdk.internal.jvmci.debug.*;

import com.oracle.graal.api.replacements.*;
import com.oracle.graal.phases.common.query.*;

@ClassSubstitution(DelimitationAPI.class)
public class DelimitationAPISubstitutions {

    @MethodSubstitution(isStatic = true)
    public static void instrumentationBegin(int target) {
        InstrumentationBeginNode.instantiate(target);
    }

    @MethodSubstitution(isStatic = true)
    public static void instrumentationEnd() {
        InstrumentationEndNode.instantiate();
    }

}
