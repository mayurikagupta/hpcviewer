package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;
import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

abstract public class AbstractTraceNode extends AbstractTreeNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4263460050236195638L;
	
	protected Vector<AbstractTreeNode> children = new Vector<AbstractTreeNode>();
		
	public AbstractTraceNode(int ID, String name, int depth, CFGGraph cfgGraph, CFGNode addrNode) {
		super(ID, name, depth, cfgGraph, addrNode);
		this.traceTime = new TraceTimeStruct();
	}
	
	protected AbstractTraceNode(AbstractTraceNode other) {
		super(other);
		
		for (int i = 0; i < other.getNumOfChildren(); i++)
			children.add(other.getChild(i).duplicate());
	}

	public long getMaxGapDurationBeforeChild(int index) {
		long duration;
		if (index == 0 && getNumOfChildren() == 0) duration = getMaxDuration();
		else if (index == 0) duration = getChild(index).getTraceTime().getStartTimeInclusive() - getTraceTime().getStartTimeExclusive() - 1;
		else if (index == getNumOfChildren()) duration = getTraceTime().getEndTimeExclusive() - getChild(index-1).getTraceTime().getEndTimeInclusive() - 1;
		else duration = getChild(index).getTraceTime().getStartTimeInclusive() - getChild(index-1).getTraceTime().getEndTimeInclusive() - 1;
		
		if (duration < 0) duration = 0;
		return duration;
	}
	
	public long getMinGapDurationBeforeChild(int index) {
		long duration;
		if (index == 0 && getNumOfChildren() == 0) duration = getMinDuration();
		else if (index == 0) duration = getChild(index).getTraceTime().getStartTimeExclusive() - getTraceTime().getStartTimeInclusive() + 1;
		else if (index == getNumOfChildren()) duration = getTraceTime().getEndTimeInclusive() - getChild(index-1).getTraceTime().getEndTimeExclusive() + 1;
		else duration = getChild(index).getTraceTime().getStartTimeExclusive() - getChild(index-1).getTraceTime().getEndTimeExclusive() + 1;
		
		if (duration < 0) duration = 0;
		return duration;
	}

	public int getNumOfChildren() {
		return children.size();
	}
	
	public AbstractTreeNode getChild(int index) {
		return children.get(index);
	}
	
	public void addChild(AbstractTreeNode node) {
		children.add(node);
	}
	
	public void updateChild(int index, AbstractTreeNode node) {
		children.set(index, node);
	}	
	
	public void shiftTime(long variable) {
		traceTime.shiftTime(variable);
		for (int i = 0; i < children.size(); i++)
			if (getChild(i) instanceof AbstractTraceNode) ((AbstractTraceNode)getChild(i)).shiftTime(variable);
			else getChild(i).getTraceTime().shiftTime(variable);
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
		if (traceTime.endTimeInclusive == traceTime.startTimeExclusive) return 0; // for dummey trace nodes
		return traceTime.endTimeInclusive - traceTime.startTimeInclusive + 1;
	}
	
	public long getMaxDuration() {
		if (traceTime.endTimeInclusive == traceTime.startTimeExclusive) return 0; // for dummey trace nodes
		return traceTime.endTimeExclusive - traceTime.startTimeExclusive - 1;
	}
	
	public String printLargeDiffNodes(int maxDepth, long durationCutoff, long totalDiff) {
		if (this.depth > maxDepth) return "";
		if (this.getDuration() < durationCutoff) return "";
		if (this.metrics.getInclusiveDiffScore() < totalDiff / TraceAnalysisUtils.diffCutoffDivider) return "";
		
		String info = "T ";

		for (int i = 0; i < depth; i++) info += "    ";

		info += name + "(" + ID + ")";
		
		info += " " + (traceTime.startTimeExclusive + traceTime.startTimeInclusive) / 2 / printDivisor + 
				" ~ " + (traceTime.endTimeInclusive + traceTime.endTimeExclusive) / 2 / printDivisor;
		
		String ret = "";
		if (totalDiff > 0) ret += diffRatioString(totalDiff);
		
		ret += "\n";
		
		for (int i = 0; i < getNumOfChildren(); i++)
			ret += getChild(i).printLargeDiffNodes(maxDepth, durationCutoff, totalDiff);
		
		if (ret.length() > 1) return info+ret;
		else return "";
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		String ret;
		if (cfgGraph == null) ret = "       ";
		else ret = Long.toHexString(cfgGraph.vma) + " ";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + traceTime.startTimeExclusive / printDivisor + "/" +traceTime.startTimeInclusive / printDivisor + 
				" ~ " + traceTime.endTimeInclusive / printDivisor + "/" + + traceTime.endTimeExclusive / printDivisor;
		
		if (metrics.getInclusiveDiffScore() != 0)
			ret += diffScoreString(weight);
		
		ret += "\n";
		
		if (getNumOfChildren() != 0 && this.getDuration() >= durationCutoff && this.depth < maxDepth)
			for (int i = 0; i < getNumOfChildren(); i++)
				ret += getChild(i).toString(maxDepth, durationCutoff, weight);
		
		return ret;
	}

}
