package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class RootTrace extends FunctionTrace {
	public RootTrace(String name) {
		super(0, name, 0, null, null);
	}
	
	public RootTrace(RootTrace other) {
		super(other);
	}
	
	public AbstractTreeNode duplicate() {
		return new RootTrace(this);
	}
}
