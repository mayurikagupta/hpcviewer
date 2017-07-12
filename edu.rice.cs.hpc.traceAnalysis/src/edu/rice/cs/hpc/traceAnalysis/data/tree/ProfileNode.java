package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.util.HashMap;


public class ProfileNode extends AbstractTreeNode {
	protected long minDurationInclusive = 0;
	protected long maxDurationInclusive = 0;
	
	protected long minDurationExclusive = 0;
	protected long maxDurationExclusive = 0;
	
	// ID to child
	HashMap<Integer, ProfileNode> childMap = new HashMap<Integer, ProfileNode>();
	
	static public ProfileNode toProfile(AbstractTreeNode node) {
		if (node instanceof ProfileNode) return new ProfileNode((ProfileNode)node);
		if (node instanceof ClusterTreeNode) return new ProfileNode((ClusterTreeNode)node);
		if (node instanceof IteratedLoopTrace) return toProfile(((IteratedLoopTrace)node).rawLoop);
		
		return new ProfileNode((AbstractTraceNode)node);
	}
	
	public ProfileNode(AbstractTraceNode trace) {
		super(trace);

		this.minDurationInclusive = trace.getMinDuration();
		this.maxDurationInclusive = trace.getMaxDuration();
		
		this.minDurationExclusive = this.minDurationInclusive;
		this.maxDurationExclusive = this.maxDurationInclusive;
		for (int k = 0; k < trace.getNumOfChildren(); k++) {
			ProfileNode profile = toProfile(trace.getChild(k));
			if (childMap.containsKey(profile.ID)) childMap.get(profile.ID).merge(profile);
			else childMap.put(profile.ID, profile);
			
			this.minDurationExclusive -= profile.maxDurationInclusive;
			this.maxDurationExclusive -= profile.minDurationInclusive;
		}
		this.minDurationExclusive = Math.max(0, this.minDurationExclusive);
		this.maxDurationExclusive = Math.max(0, this.maxDurationExclusive);
		
		this.inclusiveDiffScore = trace.inclusiveDiffScore;
		this.exclusiveDiffScore = trace.exclusiveDiffScore;
		
		setDepth(depth);
	}
	
	public ProfileNode(ClusterTreeNode cluster) {
		super(cluster);
		
		for (Cluster c: cluster.clusters) {
			ProfileNode prof = toProfile(c.getRep());
			prof.stretch(c.getWeight(), 1);
			this.merge(prof);
		}
		
		this.stretch(1, cluster.weight);
	}
	
	public ProfileNode(ProfileNode profile) {
		super(profile);
		this.minDurationExclusive = profile.minDurationExclusive;
		this.minDurationInclusive = profile.minDurationInclusive;
		this.maxDurationExclusive = profile.maxDurationExclusive;
		this.maxDurationInclusive = profile.maxDurationInclusive;
		
		for (ProfileNode child: profile.childMap.values())
			this.childMap.put(child.ID, new ProfileNode(child));
	}
	
	public void merge(ProfileNode other) {
		this.minDurationInclusive += other.minDurationInclusive;
		this.maxDurationInclusive += other.maxDurationInclusive;
		
		this.minDurationExclusive += other.minDurationExclusive;
		this.maxDurationExclusive += other.maxDurationExclusive;
		
		this.exclusiveDiffScore += other.exclusiveDiffScore;
		this.inclusiveDiffScore += other.inclusiveDiffScore;
		
		this.weight += other.weight;
		
		for (ProfileNode child : other.childMap.values()) 
			if (childMap.containsKey(child.ID)) childMap.get(child.ID).merge(child);
			else childMap.put(child.ID, child);
		
		setDepth(depth);
	}
	
	public void stretch(int multiplier, int divisor) {
		this.minDurationInclusive *= multiplier;
		this.maxDurationInclusive *= multiplier;
		this.minDurationInclusive = (this.minDurationInclusive + divisor / 2) / divisor;
		this.maxDurationInclusive = (this.maxDurationInclusive + divisor / 2) / divisor;
		
		this.minDurationExclusive = minDurationInclusive;
		this.maxDurationExclusive = maxDurationInclusive;
		
		for (ProfileNode child : childMap.values()) {
			child.stretch(multiplier, divisor);
			this.minDurationExclusive -= child.maxDurationInclusive;
			this.maxDurationExclusive -= child.minDurationInclusive;
		}
		
		this.minDurationExclusive = Math.max(0, this.minDurationExclusive);
		this.maxDurationExclusive = Math.max(0, this.maxDurationExclusive);
	}
	
	private int getNumOfChildren() {
		return childMap.size();
	}
	
	public HashMap<Integer, ProfileNode> getChildMap() {
		return childMap;
	}
	
	public long getMinExclusiveDuration() {
		return minDurationExclusive;
	}
	
	public long getMaxExclusiveDuration() {
		return maxDurationExclusive;
	}
	
	public void setDepth(int depth) {
		super.setDepth(depth);
		for (ProfileNode child : childMap.values()) 
			child.setDepth(depth+1);
	}
	
	public long getMinDuration() {
		return minDurationInclusive;
	}

	public long getMaxDuration() {
		return maxDurationInclusive;
	}

	public boolean isLeaf() {
		return childMap.isEmpty();
	}

	public void clearChildren() {
		childMap.clear();
		minDurationExclusive = minDurationInclusive;
		maxDurationExclusive = maxDurationInclusive;
	}
	
	public AbstractTreeNode duplicate() {
		return new ProfileNode(this);
	}

	public String toString(int maxDepth, long durationCutoff) {
		String ret = "P               ";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + minDurationInclusive / printDivisor + " ~ " + maxDurationInclusive / printDivisor + ", " + minDurationExclusive / printDivisor + " ~ " + maxDurationExclusive / printDivisor;
		
		if (inclusiveDiffScore != 0) {
			/*
			long t = inclusiveDiffScore * 10000 / this.getMaxDuration() / weight;
			ret += "  In-diff = " + t/100 + "." + t/1000%100 + t%1000 + "%";
			t = exclusiveDiffScore * 10000 / this.getMaxDuration() / weight;
			ret += "  Ex-diff = " + t/100 + "." + t/1000%100 + t%1000 + "%";*/
			
			long t = inclusiveDiffScore / printDivisor;
			ret += "  In-diff = " + t;
			t = exclusiveDiffScore / printDivisor;
			ret += "  Ex-diff = " + t;
		}
		
		ret += '\n';
		
		if (getNumOfChildren() != 0 && this.getDuration() >= durationCutoff && this.depth < maxDepth)
			for (ProfileNode child : childMap.values()) 
				ret += child.toString(maxDepth, durationCutoff);
		
		return ret;
	}

}
