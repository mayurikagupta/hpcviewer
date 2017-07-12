package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;

public class RawLoopTrace extends AbstractTraceNode {
	
	public RawLoopTrace(int ID, String name, int depth, CFGGraph cfgNode) {
		super(ID, name, depth, cfgNode);
	}
	
	public RawLoopTrace(RawLoopTrace other) {
		super(other);
	}
	
	public AbstractTreeNode duplicate() {
		return new RawLoopTrace(this);
	}
	
	public String toString(int maxDepth, long durationCutoff) {
		return "        R" + super.toString(maxDepth+1, durationCutoff);
	}
}
