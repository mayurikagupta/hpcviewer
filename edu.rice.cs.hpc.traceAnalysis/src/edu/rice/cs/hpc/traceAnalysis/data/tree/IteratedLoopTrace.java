package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class IteratedLoopTrace extends AbstractTraceNode {
	protected final RawLoopTrace rawLoop;
	
	public IteratedLoopTrace(RawLoopTrace rawLoop) {
		super(rawLoop.ID, rawLoop.name, rawLoop.depth, rawLoop.cfgNode);
		this.time = rawLoop.time;
		this.rawLoop = (RawLoopTrace)rawLoop.duplicate();
	}
	
	protected IteratedLoopTrace(IteratedLoopTrace other) {
		super(other);
		this.rawLoop = other.rawLoop;
	}
	
	public AbstractTreeNode duplicate() {
		return new IteratedLoopTrace(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new IteratedLoopTrace((RawLoopTrace)this.rawLoop.voidDuplicate());
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		return "        L" + super.toString(maxDepth+1, durationCutoff, weight);
	}
}
