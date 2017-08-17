package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.operator.ClusterIdentifier;
import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;


public class ClusterTreeNode extends AbstractTreeNode {
	protected final AbstractTraceNode origin;
	
	protected long minDuration;
	protected long maxDuration;

	protected Cluster[] clusters;
	private final AbstractTreeNode rep; // the averaged representative across all threads/iterations, with difference scores indicating the difference within the clusters.
	
	// Build new ClusterTreeNode from traces.
	public ClusterTreeNode(AbstractTraceNode origin, AbstractTreeNode rep, Cluster[] clusters) {
		super(origin);
		this.origin = origin;
		this.name = "Cluster of " + origin.name;
		
		this.minDuration = origin.getMinDuration();
		this.maxDuration = origin.getMaxDuration();
		
		this.clusters = clusters;
		//int numIteration = 0;
		for (int i = 0; i < clusters.length; i++) {
			clusters[i].setName("Cluster #" + i);
			clusters[i].getRep().setName("Cluster #" + i);
			//numIteration += clusters[i].getMemberSize();
		}
		
		this.rep = rep;
		rep.setName("Average for " + rep.weight + " iterations/threads of " + origin.name);
		
		this.inclusiveDiffScore = 0;
		this.exclusiveDiffScore = 0;
	}
	
	// Build new ClusterTreeNode from two existing ClusterTreeNode.
	public ClusterTreeNode(ClusterTreeNode node1, ClusterTreeNode node2, AbstractTreeNode rep, Cluster[] clusters) {
		super(node1.getID(), node1.getName(), node1.getDepth());
		
		this.origin = (AbstractTraceNode) node1.origin.voidDuplicate();
		this.weight = node1.weight + node2.weight;
		this.minDuration = ClusterIdentifier.computeWeightedAverage(node1.getMinDuration(), node1.getWeight(), node2.getMinDuration(), node2.getWeight());
		this.maxDuration = ClusterIdentifier.computeWeightedAverage(node1.getMaxDuration(), node1.getWeight(), node2.getMaxDuration(), node2.getWeight());
		
		this.clusters = clusters;
		//int numIteration = 0;
		for (int i = 0; i < clusters.length; i++) {
			clusters[i].setName("Cluster #" + i);
			clusters[i].getRep().setName("Cluster #" + i);
			//numIteration += clusters[i].getMemberSize();
		}
		//this.numIteration = numIteration;
		
		this.rep = rep;
		rep.setName("Average for " + rep.weight + " iterations/threads of " + origin.name);
		
		this.inclusiveDiffScore = this.rep.inclusiveDiffScore;
		this.exclusiveDiffScore = this.rep.exclusiveDiffScore;
	}
	
	protected ClusterTreeNode(ClusterTreeNode other) {
		super(other);
		this.minDuration = other.minDuration;
		this.maxDuration = other.maxDuration;
		this.clusters = new Cluster[other.clusters.length];
		for (int i = 0; i < clusters.length; i++)
			clusters[i] = (Cluster) other.clusters[i].duplicate();
		this.rep = other.rep.duplicate();
		//this.numIteration = other.numIteration;
		this.origin = other.origin;
	}
	
	public int getNumOfClusters() {
		return clusters.length;
	}
	
	public Cluster getCluster(int index) {
		return clusters[index];
	}
	
	public AbstractTreeNode getRep() {
		return rep;
	}
	
	public AbstractTraceNode getOrigin() {
		return origin;
	}
	
	public void setDepth(int depth) {
		super.setDepth(depth);
		rep.setDepth(depth+1);
		for (Cluster c : clusters)
			c.setDepth(depth+1);
	}
	
	public void setWeight(int weight) {
		super.setWeight(weight);
	}
	
	public boolean isLeaf() {
		return false;
	}

	public void clearChildren() {
		assert(false);
	}
	
	public void clearDiffScore() {
		super.clearDiffScore();
		rep.clearDiffScore();
		for (Cluster c : clusters)
			c.clearDiffScore();
	}
	
	public void stretchDiffScore(double multiplier, double divisor) {
		super.stretchDiffScore(multiplier, divisor);
		rep.stretchDiffScore(multiplier, divisor);
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
	
	public AbstractTreeNode voidDuplicate() {
		ClusterTreeNode ret = new ClusterTreeNode(this.origin, rep.voidDuplicate(), new Cluster[0]);
		ret.minDuration = 0;
		ret.maxDuration = 0;
		return ret;
	}
	
	public void addLabel(int ID) {
		for (Cluster cluster : clusters) 
			cluster.addLabel(ID);
	}

	public String printLargeDiffNodes(int maxDepth, long durationCutoff, TraceTimeStruct ts, long totalDiff) {
		if (this.depth > maxDepth) return "";
		if (this.getDuration() < durationCutoff) return "";
		if (this.inclusiveDiffScore < totalDiff / TraceAnalysisUtils.diffCutoffDivider) return "";
		
		String ret = "C ";

		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		if (ts != null)
			ret += " " + (ts.startTimeExclusive + ts.startTimeInclusive) / 2 / printDivisor + 
					" ~ " + (ts.endTimeInclusive + ts.endTimeExclusive) / 2 / printDivisor;
		ret += "  Duration = " + (minDuration + maxDuration) / 2 / printDivisor;
		
		String retChild = "";
		if (totalDiff > 0) retChild += diffRatioString(totalDiff);
		retChild += "\n";
		
		for (int i = 0; i < clusters.length; i++) 
			retChild += clusters[i].printLargeDiffNodes(maxDepth, durationCutoff, null, totalDiff);
		
		if (totalDiff < 0) {
			retChild += rep.printLargeDiffNodes(maxDepth, durationCutoff, null, rep.getDuration() * rep.weight * (rep.weight - 1));
			return ret + retChild;
		}
		else {
			retChild += rep.printLargeDiffNodes(maxDepth, durationCutoff, null, totalDiff);
			if (retChild.length() > 1) return ret + retChild;
			else return "";
		}
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		String ret = "C               ";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + minDuration / printDivisor + " ~ " + maxDuration / printDivisor;
		
		if (weight == 0) weight = rep.weight;
		
		if (inclusiveDiffScore != 0)
			ret += diffScoreString(weight);
		
		ret += '\n';
		
		for (int i = 0; i < clusters.length; i++) 
			ret += clusters[i].printLargeDiffNodes(maxDepth, durationCutoff, null, 0);
		
		//if (this.ID == 1598) maxDepth += 5;
		ret += rep.toString(maxDepth+1, durationCutoff, weight);
		
		//for (int i = 0; i < clusters.length; i++) 
		//	ret += clusters[i].toString(maxDepth+1, durationCutoff);
		
		return ret;
	}
}

