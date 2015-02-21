package com.oracle.graal.hotspot.replacements;

import com.oracle.graal.api.replacements.*;
import com.oracle.graal.debug.external.*;
import com.oracle.graal.hotspot.replacements.query.*;
import com.oracle.graal.nodes.spi.*;

@ClassSubstitution(CompilerDecision.class)
public class CompilerDecisionSubstitutions {

    @MacroSubstitution(forced = true, isStatic = true, macro = InstrumentationBeginNode.class)
    public static native void instrumentationBegin(boolean target);

    @MacroSubstitution(forced = true, isStatic = true, macro = InstrumentationEndNode.class)
    public static native void instrumentationEnd();

    @MacroSubstitution(forced = true, isStatic = true, macro = IsMethodCompiledNode.class)
    public static native boolean isMethodCompiled();

    @MacroSubstitution(forced = true, isStatic = true, macro = MethodNameNode.class)
    public static native String getMethodName();

}
