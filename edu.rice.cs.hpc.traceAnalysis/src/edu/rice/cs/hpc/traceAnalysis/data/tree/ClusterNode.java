package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.cluster.Cluster;

public class ClusterNode extends AbstractTreeNode {
	final protected long minDuration;
	final protected long maxDuration;

	protected Cluster[] clusters;
	final protected int numInstance;
	
	public ClusterNode(AbstractTraceNode origin, Cluster[] clusters) {
		super(origin.ID, "Cluster of " + origin.name, origin.depth);
		
		this.minDuration = origin.getMinDuration();
		this.maxDuration = origin.getMaxDuration();
		
		this.clusters = clusters;
		for (int i = 0; i < clusters.length; i++)
			clusters[i].getRep().setName("Cluster #" + i);
		
		this.numInstance = 1;
	}
	
	public ClusterNode(ClusterNode other) {
		super(other.ID, other.name, other.depth);
		this.minDuration = other.minDuration;
		this.maxDuration = other.maxDuration;
		this.clusters = new Cluster[other.clusters.length];
		for (int i = 0; i < clusters.length; i++)
			clusters[i] = new Cluster(other.clusters[i]);
		
		this.numInstance = other.numInstance;
	}
	
	public ClusterNode(ClusterNode other1, ClusterNode other2, Cluster[] clusters, long minDuration, long maxDuration) {
		super(other1.ID, other1.name, other1.depth);
		this.minDuration = minDuration;
		this.maxDuration = maxDuration;
		
		this.clusters = clusters;
		for (int i = 0; i < clusters.length; i++)
			clusters[i].getRep().setName("Cluster #" + i);
		
		this.numInstance = other1.numInstance + other2.numInstance;
	}
	
	public int getNumOfClusters() {
		return clusters.length;
	}
	
	public Cluster getCluster(int index) {
		return clusters[index];
	}

	public int getNumInstance() {
		return numInstance;
	}
	
	public boolean isLeaf() {
		return false;
	}

	public void clearChildren() {
		// TODO Auto-generated method stub
	}

	public long getMinDuration() {
		return minDuration;
	}

	public long getMaxDuration() {
		return maxDuration;
	}

	public AbstractTreeNode duplicate() {
		return new ClusterNode(this);
	}
	
	public void addLabel(int ID) {
		for (Cluster cluster : clusters) 
			cluster.addLabel(ID);
	}

	public String print(int maxDepth, long durationCutoff) {
		String ret = "C";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		String copy = "C----" + ret.substring(1);
		
		ret += name + "(" + ID + ")";
		
		ret += " " + minDuration / printDivisor + " ~ " + maxDuration / printDivisor + "\n";
		
		for (int i = 0; i < clusters.length; i++) {
			ret += copy + clusters[i].toString() + "\n";
			ret += clusters[i].getRep().print(maxDepth+1, durationCutoff);
		}
		
		return ret;
	}
}

