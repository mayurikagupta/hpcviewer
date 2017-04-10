package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.util.HashMap;

import edu.rice.cs.hpc.traceAnalysis.cluster.Cluster;

public class ProfileNode extends AbstractTreeNode {
	protected long minDurationInclusive = 0;
	protected long maxDurationInclusive = 0;
	
	protected long minDurationExclusive = 0;
	protected long maxDurationExclusive = 0;
	
	// ID to child
	HashMap<Integer, ProfileNode> childMap = new HashMap<Integer, ProfileNode>();
	
	static public ProfileNode toProfile(AbstractTreeNode node) {
		if (node instanceof ProfileNode) return new ProfileNode((ProfileNode)node);
		if (node instanceof ClusterNode) return new ProfileNode((ClusterNode)node);
		if (node instanceof IteratedLoop) return toProfile(((IteratedLoop)node).rawLoop);
		
		return new ProfileNode((AbstractTraceNode)node);
	}
	
	public ProfileNode(int ID, String name, int depth) {
		super(ID, name, depth);
	}
	
	public ProfileNode(AbstractTraceNode trace) {
		super(trace.ID, trace.name, trace.depth);

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
			//this.minDurationExclusive += trace.getMinGapDurationBeforeChild(k);
			//this.maxDurationExclusive += trace.getMaxGapDurationBeforeChild(k);
		}
		//this.minDurationExclusive += trace.getMinGapDurationBeforeChild(trace.getNumOfChildren());
		//this.maxDurationExclusive += trace.getMaxGapDurationBeforeChild(trace.getNumOfChildren());
		this.minDurationExclusive = Math.max(0, this.minDurationExclusive);
		this.maxDurationExclusive = Math.max(0, this.maxDurationExclusive);
		
		setDepth(depth);
	}
	
	public ProfileNode(ClusterNode cluster) {
		super(cluster.ID, cluster.name, cluster.depth);
		
		for (Cluster c: cluster.clusters) {
			ProfileNode prof = toProfile(c.getRep());
			prof.stretch(c.getNumOfMembers(), 1);
			this.merge(prof);
		}
		
		this.stretch(1, cluster.numInstance);
		
		/*
		this.minDurationInclusive = cluster.minDuration;
		this.maxDurationInclusive = cluster.maxDuration;
		
		this.minDurationExclusive += cluster.minDuration;
		this.maxDurationExclusive += cluster.maxDuration;
		
		this.minDurationExclusive = Math.max(0, this.minDurationExclusive);
		this.maxDurationExclusive = Math.max(0, this.maxDurationExclusive);*/
	}
	
	public ProfileNode(ProfileNode profile) {
		super(profile);
		this.minDurationExclusive = profile.minDurationExclusive;
		this.minDurationInclusive = profile.minDurationInclusive;
		this.maxDurationExclusive = profile.maxDurationExclusive;
		this.maxDurationInclusive = profile.maxDurationInclusive;
		
		for (ProfileNode child: profile.childMap.values())
			this.childMap.put(child.ID, new ProfileNode(child));
		
		setDepth(depth);
	}
	
	public void merge(ProfileNode other) {
		this.minDurationInclusive += other.minDurationInclusive;
		this.maxDurationInclusive += other.maxDurationInclusive;
		
		this.minDurationExclusive += other.minDurationExclusive;
		this.maxDurationExclusive += other.maxDurationExclusive;
		
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

	public String print(int maxDepth, long durationCutoff) {
		String ret = "P";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + minDurationInclusive / printDivisor + " ~ " + maxDurationInclusive / printDivisor + ", " + minDurationExclusive / printDivisor + " ~ " + maxDurationExclusive / printDivisor + "\n";
		
		if (getNumOfChildren() != 0 && this.getDuration() >= durationCutoff && this.depth < maxDepth)
			for (ProfileNode child : childMap.values()) 
				ret += child.print(maxDepth, durationCutoff);
		
		return ret;
	}

}
