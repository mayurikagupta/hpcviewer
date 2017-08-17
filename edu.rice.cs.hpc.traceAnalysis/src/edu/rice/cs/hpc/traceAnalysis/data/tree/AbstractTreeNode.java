package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.text.DecimalFormat;

abstract public class AbstractTreeNode {
	static protected int printDivisor = 1;
	
	final protected int ID;
	protected String name;
    protected int depth;
    
    protected int weight;
    /**
     * Scores are sum up of difference scores of (weight * (weight-1) / 2) pairs of nodes.
     */
    protected double inclusiveDiffScore = 0;
    protected double exclusiveDiffScore = 0;

	public AbstractTreeNode(int ID, String name, int depth) {
		this.ID = ID;
		this.name = name;
		this.depth = depth;
		this.weight = 1;
	}
	
	protected AbstractTreeNode(AbstractTreeNode other) {
		this.ID = other.ID;
		this.name = other.name;
		this.depth = other.depth;
		this.weight = other.weight;
		this.inclusiveDiffScore = other.inclusiveDiffScore;
		this.exclusiveDiffScore = other.exclusiveDiffScore;
	}
	
	public int getID() {
		return ID;
	}

	public String getName() {
		return name;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public double getInclusiveDiffScore() {
		return inclusiveDiffScore;
	}
	
	public double getExclusiveDiffScore() {
		return exclusiveDiffScore;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	public void setInclusiveDiffScore(double diffScore) {
		this.inclusiveDiffScore = diffScore;
	}
	
	public void setExclusiveDiffScore(double diffScore) {
		this.exclusiveDiffScore = diffScore;
	}

	abstract public boolean isLeaf();
	
	abstract public void clearChildren();
	
	public void clearDiffScore() {
		this.inclusiveDiffScore = 0;
		this.exclusiveDiffScore = 0;
	}
	
	public void stretchDiffScore(double multiplier, double divisor) {
		this.inclusiveDiffScore *= multiplier / divisor;
		this.exclusiveDiffScore *= multiplier / divisor;
	}
	
	public long getDuration() {
		return Math.max(getMinDuration(), 0);
	}
	
	abstract public long getMinDuration();
	
	abstract public long getMaxDuration();
	
	abstract public AbstractTreeNode duplicate();
	
	abstract public AbstractTreeNode voidDuplicate();
	
	abstract public String toString(int maxDepth, long durationCutoff, int weight);
	
	abstract public String printLargeDiffNodes(int maxDepth, long durationCutoff, TraceTimeStruct ts, long totalDiff);
	
	protected String diffScoreString(int weight) {
		String ret = "";
		double t = inclusiveDiffScore * 2 / (weight * (weight-1)) / printDivisor;
		ret += "  In-diff = " + Math.round(t);
		t = exclusiveDiffScore * 2 / (weight * (weight-1)) / printDivisor;
		ret += "  Ex-diff = " + Math.round(t);
		return ret;
	}
	
	protected String diffRatioString(double totalDiff) {
		String ret = "";
		double t = inclusiveDiffScore / totalDiff * 100;
		ret += "  In-diff = " + String.format("%.2f", t) + "%";
		t = exclusiveDiffScore / totalDiff * 100;
		ret += "  Ex-diff = " + String.format("%.2f", t) + "%";
		return ret;
	}
}
