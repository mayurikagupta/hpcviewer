package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.util.HashMap;

import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;


public class ProfileNode extends AbstractTreeNode {
	private static final long serialVersionUID = -474439542838312738L;
	
	protected long minDurationInclusive = 0;
	protected long maxDurationInclusive = 0;
	
	protected long minDurationExclusive = 0;
	protected long maxDurationExclusive = 0;
	
	// ID to child
	private HashMap<Integer, ProfileNode> childMap = new HashMap<Integer, ProfileNode>();
	
	static public ProfileNode toProfile(AbstractTreeNode node) {
		if (node instanceof ProfileNode) return new ProfileNode((ProfileNode)node, true);
		if (node instanceof ClusterSetNode) return new ProfileNode((ClusterSetNode)node);
		if (node instanceof IteratedLoopTrace) return toProfile(((IteratedLoopTrace)node).rawLoop);
		
		return new ProfileNode((AbstractTraceNode)node);
	}
	
	private ProfileNode(AbstractTraceNode trace) {
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
		this.minDurationInclusive = Math.max(0, this.minDurationInclusive);
		this.maxDurationInclusive = Math.max(0, this.maxDurationInclusive);
		
		setDepth(depth);
	}

	private ProfileNode(ClusterSetNode cluster) {
		super(cluster);
		/*
		for (Cluster c: cluster.clusters) {
			ProfileNode prof = toProfile(c.getRep());
			prof.stretch(c.getWeight(), 1);
			this.merge(prof);
		}*/
		ProfileNode prof = toProfile(cluster.getRep());
		prof.stretch(cluster.getRep().getWeight(), 1);
		this.childMap = prof.childMap;
		this.minDurationExclusive = prof.minDurationExclusive;
		this.minDurationInclusive = prof.minDurationInclusive;
		this.maxDurationExclusive = prof.maxDurationExclusive;
		this.maxDurationInclusive = prof.maxDurationInclusive;
		//this.inclusiveDiffScore = prof.inclusiveDiffScore;
		//this.exclusiveDiffScore = prof.exclusiveDiffScore;
		this.setWeight(cluster.weight);
		this.setDepth(cluster.getDepth());
	}
	
	private ProfileNode(ProfileNode profile, boolean initChildMap) {
		super(profile);
		this.minDurationExclusive = profile.minDurationExclusive;
		this.minDurationInclusive = profile.minDurationInclusive;
		this.maxDurationExclusive = profile.maxDurationExclusive;
		this.maxDurationInclusive = profile.maxDurationInclusive;
		
		if (initChildMap)
			for (ProfileNode child: profile.childMap.values())
				this.childMap.put(child.ID, new ProfileNode(child, initChildMap));
	}
	
	public long getMinDurationInclusive() {
		return minDurationInclusive;
	}

	public void setMinDurationInclusive(long minDurationInclusive) {
		this.minDurationInclusive = minDurationInclusive;
	}

	public long getMaxDurationInclusive() {
		return maxDurationInclusive;
	}

	public void setMaxDurationInclusive(long maxDurationInclusive) {
		this.maxDurationInclusive = maxDurationInclusive;
	}

	public long getMinDurationExclusive() {
		return minDurationExclusive;
	}
	
	public void setMinDurationExclusive(long minDurationExclusive) {
		this.minDurationExclusive = minDurationExclusive;
	}
	
	public long getMaxDurationExclusive() {
		return maxDurationExclusive;
	}

	public void setMaxDurationExclusive(long maxDurationExclusive) {
		this.maxDurationExclusive = maxDurationExclusive;
	}

	private void merge(ProfileNode other) {
		this.traceTime = null;
		
		this.minDurationInclusive += other.minDurationInclusive;
		this.maxDurationInclusive += other.maxDurationInclusive;
		
		this.minDurationExclusive += other.minDurationExclusive;
		this.maxDurationExclusive += other.maxDurationExclusive;
		
		this.metrics.add(other.metrics);
		
		for (ProfileNode child : other.childMap.values()) 
			if (childMap.containsKey(child.ID)) childMap.get(child.ID).merge(child);
			else childMap.put(child.ID, child);
		
		setDepth(depth);
	}
	
	private void stretch(int multiplier, int divisor) {
		this.minDurationInclusive *= multiplier;
		this.maxDurationInclusive *= multiplier;
		this.minDurationInclusive = (this.minDurationInclusive + (divisor-1) / 2) / divisor;
		this.maxDurationInclusive = (this.maxDurationInclusive + (divisor-1) / 2) / divisor;
		
		this.minDurationExclusive = minDurationInclusive;
		this.maxDurationExclusive = maxDurationInclusive;
		
		for (ProfileNode child : childMap.values()) {
			child.stretch(multiplier, divisor);
			this.minDurationExclusive -= child.maxDurationInclusive;
			this.maxDurationExclusive -= child.minDurationInclusive;
		}
		
		this.minDurationExclusive = Math.max(0, this.minDurationExclusive);
		this.maxDurationExclusive = Math.max(0, this.maxDurationExclusive);
		this.minDurationInclusive = Math.max(0, this.minDurationInclusive);
		this.maxDurationInclusive = Math.max(0, this.maxDurationInclusive);
	}
	
	public int getNumOfChildren() {
		return childMap.size();
	}
	
	public HashMap<Integer, ProfileNode> getChildMap() {
		return childMap;
	}
	
	public void addChild(ProfileNode child) {
		if (childMap.containsKey(child.ID)) childMap.get(child.ID).merge(child);
		else childMap.put(child.ID, child);
	}
	
	public void setDepth(int depth) {
		super.setDepth(depth);
		for (ProfileNode child : childMap.values()) 
			child.setDepth(depth+1);
	}
	
	public void setWeight(int weight) {
		super.setWeight(weight);
		for (ProfileNode child : childMap.values()) 
			child.setWeight(weight);
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
	
	public void clearDiffScore() {
		super.clearDiffScore();
		for (ProfileNode child : childMap.values())
			child.clearDiffScore();
	}
	
	public void stretchDiffScore(double multiplier, double divisor) {
		super.stretchDiffScore(multiplier, divisor);
		for (ProfileNode child : childMap.values())
			child.stretchDiffScore(multiplier, divisor);
	}
	
	public AbstractTreeNode duplicate() {
		return new ProfileNode(this, true);
	}
	
	public AbstractTreeNode voidDuplicate() {
		ProfileNode ret = new ProfileNode(this, false);
		ret.minDurationExclusive = 0;
		ret.minDurationInclusive = 0;
		ret.maxDurationExclusive = 0;
		ret.maxDurationInclusive = 0;
		ret.clearDiffScore();
		ret.childMap.clear();
		return ret;
	}

	public String printLargeDiffNodes(int maxDepth, long durationCutoff, long totalDiff) {
		if (this.depth > maxDepth) return "";
		if (this.getDuration() < durationCutoff) return "";
		if (metrics.getInclusiveDiffScore() < totalDiff / TraceAnalysisUtils.diffCutoffDivider) return "";
		
		String info = "P ";

		for (int i = 0; i < depth; i++) info += "    ";

		info += name + "(" + ID + ")";
		
		if (this.getTraceTime() != null)
			info += " " + (getTraceTime().startTimeExclusive + getTraceTime().startTimeInclusive) / 2 / printDivisor + 
					" ~ " + (getTraceTime().endTimeInclusive + getTraceTime().endTimeExclusive) / 2 / printDivisor;
		info += "  In-time = " + (minDurationInclusive + maxDurationInclusive) / 2 / printDivisor +
				"  Ex-time = " + (minDurationExclusive + maxDurationExclusive) / 2 / printDivisor;
	
		String ret = "";
		if (totalDiff > 0) ret += diffRatioString(totalDiff);
		
		ret += "\n";
		
		for (ProfileNode child : childMap.values())
			ret += child.printLargeDiffNodes(maxDepth, durationCutoff, totalDiff);
		
		if (ret.length() > 1) return info+ret;
		else return "";
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		String ret = "P";
		
		if (cfgGraph == null) ret += "       ";
		else ret += Long.toHexString(cfgGraph.vma) + "-";
		
		if (addrNode == null) ret += "        ";
		else ret += "V" + Long.toHexString(addrNode.vma)+ " ";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + minDurationInclusive / printDivisor + " ~ " + maxDurationInclusive / printDivisor + ", " + minDurationExclusive / printDivisor + " ~ " + maxDurationExclusive / printDivisor;
		
		if (metrics.getInclusiveDiffScore() != 0)
			ret += diffScoreString(weight);
		
		ret += '\n';
		
		if (getNumOfChildren() != 0 && this.getDuration() >= durationCutoff && this.depth < maxDepth)
			for (ProfileNode child : childMap.values()) 
				ret += child.toString(maxDepth, durationCutoff, weight);
		
		return ret;
	}

}
