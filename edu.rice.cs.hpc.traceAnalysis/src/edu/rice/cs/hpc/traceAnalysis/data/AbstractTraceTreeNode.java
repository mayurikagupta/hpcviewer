package edu.rice.cs.hpc.traceAnalysis.data;

import java.util.Vector;

abstract public class AbstractTraceTreeNode {
	final protected int ID;
	final protected String name;
    protected int depth;
	
	protected long startTimeExclusive;
	protected long startTimeInclusive;
	protected long endTimeInclusive;
	protected long endTimeExclusive;
	
	Vector<AbstractTraceTreeNode> children = null;

	public AbstractTraceTreeNode(int ID, String name, int depth) {
		this.ID = ID;
		this.name = name;
		this.depth = depth;
	}
	
	public boolean isLeaf() {
		return children == null;
	}
	
	public int getID() {
		return ID;
	}

	public String getName() {
		return name;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
		if (children != null)
			for (AbstractTraceTreeNode child : children)
				child.setDepth(depth+1);
	}

	public void addChild(AbstractTraceTreeNode node) {
		if (children == null) children = new Vector<AbstractTraceTreeNode>();
		children.add(node);
	}
	
	public void clearChildren() {
		if (children != null) children.clear();
	}
	
	public int getNumOfChildren() {
		if (children == null) return 0;
		return children.size();
	}
	
	public AbstractTraceTreeNode getChild(int index) {
		return children.get(index);
	}
	
	public void replaceChild(AbstractTraceTreeNode node, int index) {
		children.set(index, node);
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
	
	public long getDuration() {
		return getMinDuration();
	}
	
	public long getMinDuration() {
		return this.endTimeInclusive - this.startTimeInclusive + 1;
	}
	
	public long getMaxDuration() {
		return this.endTimeExclusive - this.startTimeExclusive - 1;
	}
	
	public String print(int maxDepth) {
		String ret = "";
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + startTimeInclusive / 1000 + " ~ " + endTimeInclusive / 1000 + "\n";
		
		if (getNumOfChildren() != 0 && depth < maxDepth)
			for (int i = 0; i < getNumOfChildren(); i++)
				ret += getChild(i).print(maxDepth);
		
		return ret;
	}
}
