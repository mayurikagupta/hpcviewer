package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;

public class FunctionTrace extends AbstractTraceNode {
	public final CFGNode ra;
	
	public FunctionTrace(int ID, String name, int depth, CFGGraph cfgNode, CFGNode ra) {
		super(ID, name, depth, cfgNode);
		this.ra = ra;
	}
	
	public FunctionTrace(FunctionTrace other) {
		super(other);
		this.ra = other.ra;
	}
	
	public FunctionTrace(RawLoopTrace loop) {
		super(loop);
		this.ra = loop.cfgNode;
	}
	
	public AbstractTreeNode duplicate() {
		return new FunctionTrace(this);
	}
	
	public String toString(int maxDepth, long durationCutoff) {
		if (ra != null)
			return "C" + Long.toHexString(ra.vma) + "-F" + super.toString(maxDepth, durationCutoff);
		else
			return "C      " + "-F" + super.toString(maxDepth, durationCutoff);
	}
}
