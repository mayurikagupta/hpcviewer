package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class RawLoop extends AbstractTraceNode {
	public RawLoop(int ID, String name, int depth) {
		super(ID, name, depth);
	}
	
	public RawLoop(RawLoop other) {
		super(other);
	}
	
	public AbstractTreeNode duplicate() {
		return new RawLoop(this);
	}
	
	public String print(int maxDepth, long durationCutoff) {
		return "*" + super.print(maxDepth+1, durationCutoff);
	}
}
