package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;

public class FunctionTrace extends AbstractTraceNode {
	public final CFGNode ra;
	
	public FunctionTrace(int ID, String name, int depth, CFGGraph cfgNode, CFGNode ra) {
		super(ID, name, depth, cfgNode);
		this.ra = ra;
	}
	
	public FunctionTrace(RawLoopTrace loop) {
		super(loop);
		this.ra = loop.cfgNode;
	}
	
	protected FunctionTrace(FunctionTrace other) {
		super(other);
		this.ra = other.ra;
	}
	
	public AbstractTreeNode duplicate() {
		return new FunctionTrace(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new FunctionTrace(this.ID, this.name, this.depth, this.cfgNode, this.ra);
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		if (ra != null)
			return "C" + Long.toHexString(ra.vma) + "-F" + super.toString(maxDepth, durationCutoff, weight);
		else
			return "C      " + "-F" + super.toString(maxDepth, durationCutoff, weight);
	}
}
