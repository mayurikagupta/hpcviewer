package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;

public class IterationTrace extends AbstractTraceNode {
	private static final long serialVersionUID = -3660573230045906119L;

	public IterationTrace(IteratedLoopTrace loop, int iterNum) {
		super(loop.getID(), "ITER_#" + iterNum, loop.getDepth(), loop.cfgGraph, loop.addrNode);
	}
	
	protected IterationTrace(IterationTrace other) {
		super(other);
	}
	
	private IterationTrace(int ID, String name, int depth, CFGGraph cfgNode, CFGNode addrNode) {
		super(ID, name, depth, cfgNode, addrNode);
	}
	
	public AbstractTreeNode duplicate() {
		return new IterationTrace(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new IterationTrace(this.ID, this.name, this.depth, this.cfgGraph, this.addrNode);
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		return "        I" + super.toString(maxDepth+1, durationCutoff, weight);
	}
}
