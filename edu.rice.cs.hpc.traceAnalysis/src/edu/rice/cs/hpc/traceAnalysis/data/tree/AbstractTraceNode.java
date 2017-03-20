package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.util.Vector;

abstract public class AbstractTraceNode extends AbstractTreeNode {
	protected TraceTimeStruct time = new TraceTimeStruct();

	protected Vector<AbstractTreeNode> children = new Vector<AbstractTreeNode>();
	
	/**
	 * Time ranges for children. 
	 * If a child is a trace node, reference in this vector should be the same object as of the TraceTimeStruct in the child.
	 * If a child is a profile node, reference in this vector can be any object of TraceTimeStruct.
	 */
	protected Vector<TraceTimeStruct> childrenTime = new Vector<TraceTimeStruct>();
	
	public AbstractTraceNode(int ID, String name, int depth) {
		super(ID, name, depth);
	}
	
	public AbstractTraceNode(AbstractTraceNode other) {
		super(other);
		
		time = other.time.duplicate();
		for (int i = 0; i < other.getNumOfChildren(); i++)
			if (other.getChild(i) instanceof AbstractTraceNode) {
				AbstractTraceNode child = (AbstractTraceNode)other.getChild(i).duplicate();
				children.add(child);
				childrenTime.add(child.time);
			} else {
				children.add(other.getChild(i).duplicate());
				childrenTime.add(other.getChildTime(i).duplicate());
			}
	}

	public TraceTimeStruct getTime(){
		return time;
	}
	
	public TraceTimeStruct getChildTime(int index) {
		return childrenTime.get(index);
	}
	
	public long getMaxGapDurationBeforeChild(int index) {
		long duration;
		if (index == 0 && getNumOfChildren() == 0) duration = getMaxDuration();
		else if (index == 0) duration = getChildTime(index).getStartTimeInclusive() - getTime().getStartTimeExclusive() - 1;
		else if (index == getNumOfChildren()) duration = getTime().getEndTimeExclusive() - getChildTime(index-1).getEndTimeInclusive() - 1;
		else duration = getChildTime(index).getStartTimeInclusive() - getChildTime(index-1).getEndTimeInclusive() - 1;
		
		if (duration < 0) duration = 0;
		return duration;
	}
	
	public long getMinGapDurationBeforeChild(int index) {
		long duration;
		if (index == 0 && getNumOfChildren() == 0) duration = getMinDuration();
		else if (index == 0) duration = getChildTime(index).getStartTimeExclusive() - getTime().getStartTimeInclusive() + 1;
		else if (index == getNumOfChildren()) duration = getTime().getEndTimeInclusive() - getChildTime(index-1).getEndTimeExclusive() + 1;
		else duration = getChildTime(index).getStartTimeExclusive() - getChildTime(index-1).getEndTimeExclusive() + 1;
		
		if (duration < 0) duration = 0;
		return duration;
	}

	public int getNumOfChildren() {
		return children.size();
	}
	
	public AbstractTreeNode getChild(int index) {
		return children.get(index);
	}
	
	public void addChild(AbstractTreeNode node, TraceTimeStruct time) {
		children.add(node);
		childrenTime.add(time);
	}
	
	public void updateChild(int index, AbstractTreeNode node) {
		children.set(index, node);
	}
	
	public void setDepth(int depth) {
		super.setDepth(depth);
		for (AbstractTreeNode child : children) 
			child.setDepth(depth+1);
	}
	
	public boolean isLeaf() {
		return children.size() == 0;
	}
	
	public void clearChildren() {
		children.clear();
		childrenTime.clear();
	}
	
	public long getMinDuration() {
		return time.endTimeInclusive - time.startTimeInclusive + 1;
	}
	
	public long getMaxDuration() {
		return time.endTimeExclusive - time.startTimeExclusive - 1;
	}
	
	public String print(int maxDepth, long durationCutoff) {
		String ret = "T";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + time.startTimeExclusive / 1000 + "/" +time.startTimeInclusive / 1000 + 
				" ~ " + time.endTimeInclusive / 1000 + "/" + + time.endTimeExclusive / 1000 + "\n";
		
		if (getNumOfChildren() != 0 && this.getMinDuration() >= durationCutoff && this.depth < maxDepth)
			for (int i = 0; i < getNumOfChildren(); i++)
				ret += getChild(i).print(maxDepth, durationCutoff);
		
		return ret;
	}
}
