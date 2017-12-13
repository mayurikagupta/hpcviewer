package edu.rice.cs.hpc.traceAnalysis.operator;

import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;

public class TraceFilter {
	public static void filterTrace(AbstractTreeNode node) {
    	if (node.getName().length()>=5 && node.getName().substring(0, 5).equals("PMPI_"))
    		node.clearChildren();
    	
    	if (node.getName().length()>=4 && node.getName().substring(0, 4).equals("MPI_"))
    		node.clearChildren();
		
		if (!(node instanceof AbstractTraceNode)) return;
		
		AbstractTraceNode trace = (AbstractTraceNode) node;
		
		for (int i = 0; i < trace.getNumOfChildren(); i++)
			filterTrace(trace.getChild(i));
	}
}
