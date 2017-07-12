package edu.rice.cs.hpc.traceAnalysis.data.tree;


public class ClusterTreeNode extends AbstractTreeNode {
	final protected long minDuration;
	final protected long maxDuration;

	protected Cluster[] clusters;
	
	public ClusterTreeNode(AbstractTraceNode origin, Cluster[] clusters) {
		super(origin);
		this.name = "Cluster of " + origin.name;
		
		this.minDuration = origin.getMinDuration();
		this.maxDuration = origin.getMaxDuration();
		
		this.clusters = clusters;
		for (int i = 0; i < clusters.length; i++)
			clusters[i].getRep().setName("Cluster #" + i);
	}
	
	public ClusterTreeNode(ClusterTreeNode other) {
		super(other);
		this.minDuration = other.minDuration;
		this.maxDuration = other.maxDuration;
		this.clusters = new Cluster[other.clusters.length];
		for (int i = 0; i < clusters.length; i++)
			clusters[i] = new Cluster(other.clusters[i]);
	}
	
	public ClusterTreeNode(ClusterTreeNode other1, ClusterTreeNode other2, Cluster[] clusters, long minDuration, long maxDuration) {
		super(other1);
		this.weight = other1.weight + other2.weight;
		this.minDuration = minDuration;
		this.maxDuration = maxDuration;
		
		this.clusters = clusters;
		for (int i = 0; i < clusters.length; i++)
			clusters[i].getRep().setName("Cluster #" + i);
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
		assert(false);
	}

	public long getMinDuration() {
		return minDuration;
	}

	public long getMaxDuration() {
		return maxDuration;
	}

	public AbstractTreeNode duplicate() {
		return new ClusterTreeNode(this);
	}
	
	public void addLabel(int ID) {
		for (Cluster cluster : clusters) 
			cluster.addLabel(ID);
	}

	public String toString(int maxDepth, long durationCutoff) {
		String ret = "C               ";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + minDuration / printDivisor + " ~ " + maxDuration / printDivisor;
		
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
		
		for (int i = 0; i < clusters.length; i++) 
			ret += clusters[i].toString(maxDepth, durationCutoff);
		
		return ret;
	}
}

