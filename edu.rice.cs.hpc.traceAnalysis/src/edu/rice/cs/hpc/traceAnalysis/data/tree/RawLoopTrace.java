package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;

public class RawLoopTrace extends AbstractTraceNode {
	private static final long serialVersionUID = 3459129701847983180L;

	public RawLoopTrace(int ID, String name, int depth, CFGGraph cfgGraph) {
		super(ID, name, depth, cfgGraph, cfgGraph);
	}
	
	protected RawLoopTrace(RawLoopTrace other) {
		super(other);
	}
	
	public AbstractTreeNode duplicate() {
		return new RawLoopTrace(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new RawLoopTrace(ID, name, depth, cfgGraph);
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		return "        R" + super.toString(maxDepth+1, durationCutoff, weight);
	}
}
