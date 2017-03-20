package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class RootTrace extends FunctionTrace {
	public RootTrace() {
		super(0, "ROOT", 0);
	}
	
	public RootTrace(RootTrace other) {
		super(other);
	}
	
	public AbstractTreeNode duplicate() {
		return new RootTrace(this);
	}
}
