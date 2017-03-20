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
		if (node instanceof ClusterNode) return toProfile(((ClusterNode)node).origin);
		if (node instanceof IteratedLoop) return toProfile(((IteratedLoop)node).rawLoop);
		
		return new ProfileNode((AbstractTraceNode)node);
	}
	
	public ProfileNode(AbstractTraceNode trace) {
		super(trace.ID, trace.name, trace.depth);

		this.minDurationInclusive = trace.getMinDuration();
		this.maxDurationInclusive = trace.getMaxDuration();
		for (int k = 0; k < trace.getNumOfChildren(); k++) {
			ProfileNode profile = toProfile(trace.getChild(k));
			if (childMap.containsKey(profile.ID)) childMap.get(profile.ID).merge(profile);
			else childMap.put(profile.ID, profile);
			this.minDurationExclusive += trace.getMinGapDurationBeforeChild(k);
			this.maxDurationExclusive += trace.getMaxGapDurationBeforeChild(k);
		}
		this.minDurationExclusive += trace.getMinGapDurationBeforeChild(trace.getNumOfChildren());
		this.maxDurationExclusive += trace.getMaxGapDurationBeforeChild(trace.getNumOfChildren());
		
		setDepth(depth);
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
		
		ret += " " + minDurationInclusive / 1000 + " ~ " + maxDurationInclusive / 1000 + ", " + minDurationExclusive / 1000 + " ~ " + maxDurationExclusive / 1000 + "\n";
		
		if (getNumOfChildren() != 0 && this.getMinDuration() >= durationCutoff && this.depth < maxDepth)
			for (ProfileNode child : childMap.values()) 
				ret += child.print(maxDepth, durationCutoff);
		
		return ret;
	}

}
