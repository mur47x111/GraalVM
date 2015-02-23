package com.oracle.graal.hotspot.replacements;

import com.oracle.graal.api.replacements.*;
import com.oracle.graal.debug.external.*;
import com.oracle.graal.hotspot.replacements.query.*;
import com.oracle.graal.nodes.spi.*;

@ClassSubstitution(CompilerDecision.class)
public class CompilerDecisionSubstitutions {

    @MacroSubstitution(forced = true, isStatic = true, macro = InstrumentationBeginNode.class)
    public static native void instrumentationBegin(int target);

    @MacroSubstitution(forced = true, isStatic = true, macro = InstrumentationEndNode.class)
    public static native void instrumentationEnd();

    @MacroSubstitution(forced = true, isStatic = true, macro = ObservedReferenceNode.class)
    public static native Object observedReference(Object object);

    @MacroSubstitution(forced = true, isStatic = true, macro = IsAllocationVirtualNode.class)
    public static native boolean isAllocationVirtual();

    @MacroSubstitution(forced = true, isStatic = true, macro = IsMethodCompiledNode.class)
    public static native boolean isMethodCompiled();

    @MacroSubstitution(forced = true, isStatic = true, macro = IsMethodInlinedNode.class)
    public static native boolean isMethodInlined();

    @MacroSubstitution(forced = true, isStatic = true, macro = IsCallsiteInlinedNode.class)
    public static native boolean isCallsiteInlined();

    @MacroSubstitution(forced = true, isStatic = true, macro = MethodNameNode.class)
    public static native String getMethodName();

    @MacroSubstitution(forced = true, isStatic = true, macro = RootNameNode.class)
    public static native String getRootName();

}
