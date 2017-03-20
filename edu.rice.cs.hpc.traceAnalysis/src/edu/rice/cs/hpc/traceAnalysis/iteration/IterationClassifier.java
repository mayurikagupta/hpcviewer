package edu.rice.cs.hpc.traceAnalysis.iteration;

import edu.rice.cs.hpc.traceAnalysis.cluster.ClusterIdentifier;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IteratedLoop;

public class IterationClassifier {
	public static AbstractTreeNode ClasifyLoops(AbstractTreeNode node) {
		if (!(node instanceof AbstractTraceNode)) return null;
		AbstractTraceNode trace = (AbstractTraceNode) node;
			
		for (int i = 0; i < trace.getNumOfChildren(); i++) {
			AbstractTreeNode newNode = ClasifyLoops(trace.getChild(i));
			if (newNode != null) trace.updateChild(i, newNode);
		}
		
		if (trace instanceof IteratedLoop) {
			ClusterNode cluster = ClusterIdentifier.findCluster(trace);
			
			if (trace.getID() == 69617 || trace.getID() == 23299) {
				if (cluster != null) 
					System.out.print(cluster.print(cluster.getDepth(), 0));
				else
					System.out.println(trace.getName()+"("+trace.getID()+") not clustered.");
				System.out.println("-----------------------------------");
			}
			
			return cluster;
		}
		
		return null;
	}
	
}
