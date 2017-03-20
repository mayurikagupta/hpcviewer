package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class Iteration extends AbstractTraceNode {
	//protected final int iterNum;
	
	public Iteration(IteratedLoop loop, int iterNum) {
		super(loop.getID(), "ITER #" + iterNum, loop.getDepth());
		//this.iterNum = iterNum;
	}
	
	public Iteration(Iteration other) {
		super(other);
		//this.iterNum = other.iterNum;
	}
	
	public AbstractTreeNode duplicate() {
		return new Iteration(this);
	}
	
	public String print(int maxDepth, long durationCutoff) {
		return "I" + super.print(maxDepth+1, durationCutoff).substring(1);
	}
}
