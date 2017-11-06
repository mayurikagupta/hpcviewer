package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.io.Serializable;

public class TreeNodeMetrics implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -2509888591932616974L;
	
	
	private double inclusiveDiffScore = 0;
	private double exclusiveDiffScore = 0;
    
	private long minDuration = 0;
	private long maxDuration = 0;
	private long totalDuration = 0; //TODO overflow?
    
    public TreeNodeMetrics duplicate() {
    	TreeNodeMetrics ret = new TreeNodeMetrics();
    	ret.inclusiveDiffScore = this.inclusiveDiffScore;
    	ret.exclusiveDiffScore = this.exclusiveDiffScore;
    	ret.minDuration = this.minDuration;
    	ret.maxDuration = this.maxDuration;
    	ret.totalDuration = this.totalDuration;
    	
    	return ret;
    }
    
	public double getInclusiveDiffScore() {
		return inclusiveDiffScore;
	}

	public void setInclusiveDiffScore(double inclusiveDiffScore) {
		this.inclusiveDiffScore = inclusiveDiffScore;
	}

	public double getExclusiveDiffScore() {
		return exclusiveDiffScore;
	}

	public void setExclusiveDiffScore(double exclusiveDiffScore) {
		this.exclusiveDiffScore = exclusiveDiffScore;
	}

	public void add(TreeNodeMetrics other) {
		this.inclusiveDiffScore += other.inclusiveDiffScore;
		this.exclusiveDiffScore += other.exclusiveDiffScore;
		this.minDuration += other.minDuration;
		this.maxDuration += other.maxDuration;
		this.totalDuration += other.totalDuration;
	}
	
	public void merge(TreeNodeMetrics other) {
		this.inclusiveDiffScore += other.inclusiveDiffScore;
		this.exclusiveDiffScore += other.exclusiveDiffScore;
		this.minDuration = Math.min(this.minDuration, other.minDuration);
		this.maxDuration = Math.max(this.maxDuration, other.maxDuration);
		this.totalDuration += other.totalDuration;
	}
}
