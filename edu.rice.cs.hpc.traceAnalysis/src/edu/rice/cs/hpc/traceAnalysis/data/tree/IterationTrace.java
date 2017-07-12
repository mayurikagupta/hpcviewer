package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class IterationTrace extends AbstractTraceNode {
	//protected final int iterNum;
	
	public IterationTrace(IteratedLoopTrace loop, int iterNum) {
		super(loop.getID(), "ITER #" + iterNum, loop.getDepth(), loop.cfgNode);
		//this.iterNum = iterNum;
	}
	
	public IterationTrace(IterationTrace other) {
		super(other);
		//this.iterNum = other.iterNum;
	}
	
	public AbstractTreeNode duplicate() {
		return new IterationTrace(this);
	}
	
	public String toString(int maxDepth, long durationCutoff) {
		return "        I" + super.toString(maxDepth+1, durationCutoff);
	}
}
