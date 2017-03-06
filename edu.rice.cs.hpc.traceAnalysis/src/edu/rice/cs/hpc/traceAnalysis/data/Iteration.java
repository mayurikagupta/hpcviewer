package edu.rice.cs.hpc.traceAnalysis.data;

public class Iteration extends AbstractTraceTreeNode {
	public Iteration(RawLoopWithIteration loop, int iterNum) {
		super(loop.getID(), "ITER #" + iterNum, loop.getDepth()+1);
	}
	
	public String print(int maxDepth) {
		return "+" + super.print(maxDepth+1);
	}
}
