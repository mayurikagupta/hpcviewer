package edu.rice.cs.hpc.traceAnalysis.data;

import java.util.Vector;

import edu.rice.cs.hpc.data.experiment.scope.Scope;

public class TraceTreeNode {
	private Scope scope;
	private int depth;
	
	private long startTimeExclusive;
	private long startTimeInclusive;
	private long endTimeInclusive;
	private long endTimeExclusive;
	
	private long startSampleInclusive;
	private long endSampleInclusive;
	
	Vector<TraceTreeNode> children = null;

	public TraceTreeNode(Scope scope, int depth) {
		this.scope = scope;
		this.depth = depth;
	}
	
	public boolean isRoot() {
		return depth == -1;
	}
	
	public boolean isLeaf() {
		return children == null;
	}
	
	public void addChild(TraceTreeNode node) {
		if (children == null) children = new Vector<TraceTreeNode>();
		children.add(node);
	}
	
	public int getNumOfChildren() {
		if (children == null) return 0;
		return children.size();
	}
	
	public TraceTreeNode getChild(int index) {
		return children.get(index);
	}

	public Scope getScope() {
		return scope;
	}

	public int getDepth() {
		return depth;
	}

	public long getStartTimeExclusive() {
		return startTimeExclusive;
	}

	public void setStartTimeExclusive(long startTimeExclusive) {
		this.startTimeExclusive = startTimeExclusive;
	}

	public long getStartTimeInclusive() {
		return startTimeInclusive;
	}

	public void setStartTimeInclusive(long startTimeInclusive) {
		this.startTimeInclusive = startTimeInclusive;
	}

	public long getEndTimeInclusive() {
		return endTimeInclusive;
	}

	public void setEndTimeInclusive(long endTimeInclusive) {
		this.endTimeInclusive = endTimeInclusive;
	}

	public long getEndTimeExclusive() {
		return endTimeExclusive;
	}

	public void setEndTimeExclusive(long endTimeExclusive) {
		this.endTimeExclusive = endTimeExclusive;
	}
	
	public long getStartSampleInclusive() {
		return startSampleInclusive;
	}

	public void setStartSampleInclusive(long startSampleInclusive) {
		this.startSampleInclusive = startSampleInclusive;
	}

	public long getEndSampleInclusive() {
		return endSampleInclusive;
	}

	public void setEndSampleInclusive(long endSampleInclusive) {
		this.endSampleInclusive = endSampleInclusive;
	}
	
	public long getNumSamples() {
		return this.endSampleInclusive - this.startSampleInclusive + 1;
	}

	public String print(int maxDepth) {
		String ret = "";
		for (int i = 0; i <= depth; i++) ret += "    ";
		
		if (scope == null) ret += "ROOT";
		else ret += scope.getName() + "(" + scope.getCCTIndex() + ")";
		
		ret += " " + startTimeInclusive / 1000 + " ~ " + endTimeInclusive / 1000 + "   ";
		ret += " #" + startSampleInclusive + " ~ #" + endSampleInclusive + '\n';
		
		if (getNumOfChildren() != 0 && depth < maxDepth)
			for (int i = 0; i < getNumOfChildren(); i++)
				ret += getChild(i).print(maxDepth);
		
		return ret;
	}
}
