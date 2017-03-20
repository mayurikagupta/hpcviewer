package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.cluster.Cluster;

public class ClusterNode extends AbstractTreeNode {
	protected final AbstractTraceNode origin;

	protected Cluster[] clusters;
	
	public ClusterNode(AbstractTraceNode origin, Cluster[] clusters) {
		super(origin.ID, "Cluster of " + origin.name, origin.depth);
		this.origin = (AbstractTraceNode)origin.duplicate();
		
		this.clusters = clusters;
	}
	
	public ClusterNode(ClusterNode other) {
		super(other.ID, other.name, other.depth);
		this.origin = (AbstractTraceNode)other.origin.duplicate();
		this.clusters = new Cluster[other.clusters.length];
		for (int i = 0; i < clusters.length; i++)
			clusters[i] = new Cluster(other.clusters[i]);
	}

	public int getNumOfClusters() {
		return clusters.length;
	}
	
	public Cluster getCluster(int index) {
		return clusters[index];
	}

	public boolean isLeaf() {
		return false;
	}

	public void clearChildren() {
		// TODO Auto-generated method stub
	}

	public long getMinDuration() {
		return origin.getMinDuration();
	}

	public long getMaxDuration() {
		return origin.getMaxDuration();
	}

	public AbstractTreeNode duplicate() {
		return new ClusterNode(this);
	}

	public String print(int maxDepth, long durationCutoff) {
		String ret = "C";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		String copy = "C--" + ret.substring(1);
		
		ret += name + "(" + ID + ")";
		
		ret += " " + origin.time.startTimeExclusive / 1000 + "/" + origin.time.startTimeInclusive / 1000 + 
				" ~ " + origin.time.endTimeInclusive / 1000 + "/" + + origin.time.endTimeExclusive / 1000 + "\n";
		
		for (int i = 0; i < clusters.length; i++) {
			ret += copy + clusters[i].toString() + "\n";
			ret += clusters[i].getRep().print(maxDepth+1, durationCutoff);
		}
		
		return ret;
	}
}

