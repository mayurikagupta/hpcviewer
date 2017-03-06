package edu.rice.cs.hpc.traceAnalysis.iteration;

import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.AbstractTraceTreeNode;

public class IterationCluster {
	private AbstractTraceTreeNode iteration;
	private Vector<Integer> iterNums = new Vector<Integer>();
	
	public IterationCluster(AbstractTraceTreeNode iteration, int iterNum) {
		this.iteration = iteration;
		this.iterNums.add(iterNum);
	}
}
