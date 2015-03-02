package com.oracle.graal.hotspot.replacements.query;

import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.replacements.nodes.*;

@NodeInfo
public abstract class ICGMacroNode extends MacroNode {

    public static final NodeClass<ICGMacroNode> TYPE = NodeClass.create(ICGMacroNode.class);

    public ICGMacroNode(NodeClass<? extends ICGMacroNode> c, Invoke invoke) {
        super(c, invoke);
    }

    @Override
    public void lower(LoweringTool tool) {
    }

}
