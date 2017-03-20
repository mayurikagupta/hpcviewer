package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class FunctionTrace extends AbstractTraceNode {
	public FunctionTrace(int ID, String name, int depth) {
		super(ID, name, depth);
	}
	
	public FunctionTrace(FunctionTrace other) {
		super(other);
	}
	
	public AbstractTreeNode duplicate() {
		return new FunctionTrace(this);
	}
}
