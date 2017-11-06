package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;

public class FunctionTrace extends AbstractTraceNode {
	private static final long serialVersionUID = 5162368077246918208L;
	
	public FunctionTrace(int ID, String name, int depth, CFGGraph cfgGraph, CFGNode ra) {
		super(ID, name, depth, cfgGraph, ra);
	}
	
	public FunctionTrace(RawLoopTrace loop) {
		super(loop);
	}
	
	protected FunctionTrace(FunctionTrace other) {
		super(other);
	}
	
	public AbstractTreeNode duplicate() {
		return new FunctionTrace(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new FunctionTrace(this.ID, this.name, this.depth, this.cfgGraph, this.addrNode);
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		if (addrNode != null)
			return "C" + Long.toHexString(addrNode.vma) + "-R" + super.toString(maxDepth, durationCutoff, weight);
		else
			return "C      " + "-R" + super.toString(maxDepth, durationCutoff, weight);
	}
}
