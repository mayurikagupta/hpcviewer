package edu.rice.cs.hpc.traceAnalysis.data.tree;

abstract public class AbstractTreeNode {
	static protected int printDivisor = 1;
	
	final protected int ID;
	protected String name;
    protected int depth;
    
    protected int weight;
    /**
     * Scores are sum up of difference scores of (weight * (weight-1) / 2) pairs of nodes.
     */
    protected long inclusiveDiffScore = 0;
    protected long exclusiveDiffScore = 0;

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
	
	public long getInclusiveDiffScore() {
		return inclusiveDiffScore;
	}
	
	public long getExclusiveDiffScore() {
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
	
	public void setInclusiveDiffScore(long diffScore) {
		this.inclusiveDiffScore = diffScore;
	}
	
	public void setExclusiveDiffScore(long diffScore) {
		this.exclusiveDiffScore = diffScore;
	}

	abstract public boolean isLeaf();
	
	abstract public void clearChildren();
	
	public void clearDiffScore() {
		this.inclusiveDiffScore = 0;
		this.exclusiveDiffScore = 0;
	}
	
	public void stretchDiffScore(int multiplier, int divisor) {
		this.inclusiveDiffScore *= multiplier;
		this.exclusiveDiffScore *= multiplier;
		this.inclusiveDiffScore = (this.inclusiveDiffScore + (divisor-1) / 2) / divisor;
		this.exclusiveDiffScore = (this.exclusiveDiffScore + (divisor-1) / 2) / divisor;
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
		long t = inclusiveDiffScore * 2 / (weight * (weight-1)) / printDivisor;
		ret += "  In-diff = " + t;
		t = exclusiveDiffScore * 2 / (weight * (weight-1)) / printDivisor;
		ret += "  Ex-diff = " + t;
		return ret;
	}
	
	protected String diffRatioString(long totalDiff) {
		String ret = "";
		long t = inclusiveDiffScore * 10000 / totalDiff;
		ret += "  In-diff = " + t/100 + "." + t/10%10 + t%10 + "%";
		t = exclusiveDiffScore * 10000/ totalDiff;
		ret += "  Ex-diff = " + t/100 + "." + t/10%10 + t%10 + "%";
		return ret;
	}
}
