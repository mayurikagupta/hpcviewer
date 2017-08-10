package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;

public class RawLoopTrace extends AbstractTraceNode {
	
	public RawLoopTrace(int ID, String name, int depth, CFGGraph cfgNode) {
		super(ID, name, depth, cfgNode);
	}
	
	protected RawLoopTrace(RawLoopTrace other) {
		super(other);
	}
	
	public AbstractTreeNode duplicate() {
		return new RawLoopTrace(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new RawLoopTrace(ID, name, depth, cfgNode);
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		return "        R" + super.toString(maxDepth+1, durationCutoff, weight);
	}
}
