package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;

public class IterationTrace extends AbstractTraceNode {
	//protected final int iterNum;
	
	public IterationTrace(IteratedLoopTrace loop, int iterNum) {
		super(loop.getID(), "ITER_#" + iterNum, loop.getDepth(), loop.cfgNode);
		//this.iterNum = iterNum;
	}
	
	protected IterationTrace(IterationTrace other) {
		super(other);
		//this.iterNum = other.iterNum;
	}
	
	private IterationTrace(int ID, String name, int depth, CFGGraph cfgNode) {
		super(ID, name, depth, cfgNode);
	}
	
	public AbstractTreeNode duplicate() {
		return new IterationTrace(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new IterationTrace(this.ID, this.name, this.depth, this.cfgNode);
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		return "        I" + super.toString(maxDepth+1, durationCutoff, weight);
	}
}
