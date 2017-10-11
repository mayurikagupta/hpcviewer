package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGCall;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;
import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

abstract public class AbstractTraceNode extends AbstractTreeNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4263460050236195638L;
	protected TraceTimeStruct time = new TraceTimeStruct();
	public transient CFGGraph cfgNode;
	
	protected Vector<AbstractTreeNode> children = new Vector<AbstractTreeNode>();
	
	/**
	 * Time ranges for children. 
	 * If a child is a trace node, reference in this vector should be the same object as of the TraceTimeStruct in the child.
	 * If a child is a profile node, reference in this vector can be any object of TraceTimeStruct.
	 */
	protected Vector<TraceTimeStruct> childrenTime = new Vector<TraceTimeStruct>();
	
	/**
	 * CFGNode references for children.
	 * If a child represent a function call, the reference should be CFGCall
	 * If a child represent a loop, the reference should be CFGLoop
	 */
	protected transient Vector<CFGNode> childrenCFGNode = new Vector<CFGNode>();
	
	public AbstractTraceNode(int ID, String name, int depth, CFGGraph cfgNode) {
		super(ID, name, depth);
		this.cfgNode = cfgNode;
	}
	
	protected AbstractTraceNode(AbstractTraceNode other) {
		super(other);
		cfgNode = other.cfgNode;
		
		time = other.time.duplicate();
		for (int i = 0; i < other.getNumOfChildren(); i++)
			if (other.getChild(i) instanceof AbstractTraceNode) {
				AbstractTraceNode child = (AbstractTraceNode)other.getChild(i).duplicate();
				children.add(child);
				childrenTime.add(child.time);
				childrenCFGNode.add(other.getChildCFGNode(i));
			} else {
				children.add(other.getChild(i).duplicate());
				childrenTime.add(other.getChildTime(i) == null ? null : other.getChildTime(i).duplicate());
				childrenCFGNode.add(other.getChildCFGNode(i));
			}
	}

	public TraceTimeStruct getTime(){
		return time;
	}
	
	public void setTime(TraceTimeStruct time) {
		this.time = time;
	}
	
	public TraceTimeStruct getChildTime(int index) {
		return childrenTime.get(index);
	}
	
	public CFGNode getChildCFGNode(int index) {
		return childrenCFGNode.get(index);
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
	
	public void addChild(AbstractTreeNode node, TraceTimeStruct time, CFGNode cfgNode) {
		children.add(node);
		childrenTime.add(time);
		childrenCFGNode.add(cfgNode);
		
		if (node instanceof AbstractTraceNode) 
			assert (time == ((AbstractTraceNode)node).time);
		
	}
	
	public void updateChild(int index, AbstractTreeNode node) {
		children.set(index, node);

		if (node instanceof AbstractTraceNode) {
			childrenTime.set(index, ((AbstractTraceNode) node).getTime());
		}
		
		//TODO update childrenCFGNode?
	}
	/*
	public void moveChild(int origin, int dest) {
		assert(origin > dest);
		
		long duration = childrenTime.get(origin).endTimeExclusive - childrenTime.get(origin-1).endTimeExclusive;
		
		long headEndTimeInclusive;
		long headEndTimeExclusive;
		long tailEndTimeInclusive = childrenTime.get(origin).endTimeInclusive;
		long tailEndTimeExslusive = childrenTime.get(origin).endTimeExclusive;
		
		if (dest == 0) {
			headEndTimeInclusive = time.startTimeExclusive;
			headEndTimeExclusive = time.startTimeInclusive;
		}
		else {
			headEndTimeInclusive = childrenTime.get(dest-1).endTimeInclusive;
			headEndTimeExclusive = childrenTime.get(dest-1).endTimeExclusive;
		}
		
		AbstractTreeNode child = children.get(origin);
		TraceTimeStruct childTime = childrenTime.get(origin);
		
		// push children between dest and origin backward
		for (int i = origin; i > dest; i--) {
			children.set(i, children.get(i-1));
			childrenTime.set(i, childrenTime.get(i-1));
			
			if (children.get(i) instanceof AbstractTraceNode) ((AbstractTraceNode)children.get(i)).shiftTime(duration);
			else childrenTime.get(i).shiftTime(duration);
		}
		
		//TODO adjust the endTimeExclusive of the last child.
		
		children.set(dest, child);
		childrenTime.set(dest, childTime);
		
		long adjustment = headEndTimeExclusive + duration - childTime.endTimeExclusive;
	}
	
	private void adjustStartTimeExclusive(long updated) {
		if (childrenTime.get(0).startTimeExclusive == time.startTimeExclusive)
			if (children.get(0) instanceof AbstractTraceNode) ((AbstractTraceNode)children.get(0)).adjustStartTimeExclusive(updated);
			else childrenTime.get(0).startTimeExclusive = updated;
		
		time.startTimeExclusive = updated;
	}
	
	*/
	
	
	public void shiftTime(long variable) {
		time.shiftTime(variable);
		for (int i = 0; i < children.size(); i++)
			if (children.get(i) instanceof AbstractTraceNode) ((AbstractTraceNode)children.get(i)).shiftTime(variable);
			else childrenTime.get(i).shiftTime(variable);
	}
	
	public void setDepth(int depth) {
		super.setDepth(depth);
		for (AbstractTreeNode child : children) 
			child.setDepth(depth+1);
	}
	
	public void setWeight(int weight) {
		super.setWeight(weight);
		for (AbstractTreeNode node : children)
			node.setWeight(weight);
	}
	
	public boolean isLeaf() {
		return children.size() == 0;
	}
	
	public void clearChildren() {
		children.clear();
		childrenTime.clear();
		childrenCFGNode.clear();
	}
	
	public void clearDiffScore() {
		super.clearDiffScore();
		for (AbstractTreeNode node : children)
			node.clearDiffScore();
	}
	
	public void stretchDiffScore(double multiplier, double divisor) {
		super.stretchDiffScore(multiplier, divisor);
		for (AbstractTreeNode node : children)
			node.stretchDiffScore(multiplier, divisor);
	}
	
	public long getMinDuration() {
		if (time.endTimeInclusive == time.startTimeExclusive) return 0; // for dummey trace nodes
		return time.endTimeInclusive - time.startTimeInclusive + 1;
	}
	
	public long getMaxDuration() {
		if (time.endTimeInclusive == time.startTimeExclusive) return 0; // for dummey trace nodes
		return time.endTimeExclusive - time.startTimeExclusive - 1;
	}
	
	public String printLargeDiffNodes(int maxDepth, long durationCutoff, TraceTimeStruct ts, long totalDiff) {
		if (this.depth > maxDepth) return "";
		if (this.getDuration() < durationCutoff) return "";
		if (this.inclusiveDiffScore < totalDiff / TraceAnalysisUtils.diffCutoffDivider) return "";
		
		String info = "T ";

		for (int i = 0; i < depth; i++) info += "    ";

		info += name + "(" + ID + ")";
		
		info += " " + (time.startTimeExclusive + time.startTimeInclusive) / 2 / printDivisor + 
				" ~ " + (time.endTimeInclusive + time.endTimeExclusive) / 2 / printDivisor;
		
		String ret = "";
		if (totalDiff > 0) ret += diffRatioString(totalDiff);
		
		ret += "\n";
		
		for (int i = 0; i < getNumOfChildren(); i++)
			ret += getChild(i).printLargeDiffNodes(maxDepth, durationCutoff, getChildTime(i), totalDiff);
		
		if (ret.length() > 1) return info+ret;
		else return "";
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		String ret;
		if (cfgNode == null) ret = "       ";
		else ret = Long.toHexString(cfgNode.vma) + " ";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + time.startTimeExclusive / printDivisor + "/" +time.startTimeInclusive / printDivisor + 
				" ~ " + time.endTimeInclusive / printDivisor + "/" + + time.endTimeExclusive / printDivisor;
		
		if (inclusiveDiffScore != 0)
			ret += diffScoreString(weight);
		
		ret += "\n";
		
		if (getNumOfChildren() != 0 && this.getDuration() >= durationCutoff && this.depth < maxDepth)
			for (int i = 0; i < getNumOfChildren(); i++)
				ret += getChild(i).toString(maxDepth, durationCutoff, weight);
		
		return ret;
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(cfgNode == null ? null : cfgNode.toString());
		
		Vector<String> CFGStrings = new Vector<String>();
		for (CFGNode node : childrenCFGNode)
			CFGStrings.add(node == null ? null : node.toString());
		out.writeObject(CFGStrings);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		String str = (String) in.readObject();
		this.cfgNode = (CFGGraph) rebuildCFGNode(str);
		
		@SuppressWarnings("unchecked")
		Vector<String> CFGStrings = (Vector<String>) in.readObject();
		childrenCFGNode = new Vector<CFGNode>();
		for (String s : CFGStrings)
			childrenCFGNode.add(rebuildCFGNode(s));
	}
	
	protected CFGNode rebuildCFGNode(String str) {
		if (str == null) return null;
			
		String[] split = str.split("_");
		assert(split[1].subSequence(0, 2).equals("0x"));
		long addr = Long.decode(split[1]);
		if (split[0].equals("loop"))
			return TraceAnalysisUtils.lookupCFGLoop(addr);
		else if (split[0].equals("func"))
			return TraceAnalysisUtils.lookupCFGFunc(addr);
		else
			return new CFGCall(addr);
	}
}
