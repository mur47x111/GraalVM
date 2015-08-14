package com.oracle.graal.phases.common.query;

import java.util.*;

import com.oracle.graal.nodes.*;

public class ICGUtil {

    public static Set<StructuredGraph> getAllICGs(StructuredGraph graph) {
        Set<StructuredGraph> icgs = new HashSet<>();
        for (InstrumentationNode instrumentation : graph.getNodes().filter(InstrumentationNode.class)) {
            icgs.add(instrumentation.icg());
        }
        return icgs;
    }

}
