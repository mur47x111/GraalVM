package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.nodes.*;

@NodeInfo
public abstract class ICGMacroNode extends MacroNode {

    public ICGMacroNode(Invoke invoke) {
        super(invoke);
    }

    @Override
    public void lower(LoweringTool tool) {
    }

}
