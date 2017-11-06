package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

public class ClusterSetNode extends AbstractTreeNode {
	private static final long serialVersionUID = -1173018873034372326L;

	protected final String originFileName;
	protected final AbstractTraceNode originVoidDuplicate;
	//private AbstractTraceNode origin = null;
	
	protected long minDuration;
	protected long maxDuration;

	protected Cluster[] clusters;
	private final AbstractTreeNode rep; // the averaged representative across all threads/iterations, with difference scores indicating the difference within the clusters.
	
	// Build new ClusterTreeNode from traces.
	public ClusterSetNode(AbstractTraceNode origin, String originFileName, AbstractTreeNode rep, Cluster[] clusters) {
		super(origin);
		this.name = "Cluster of " + origin.name;
		
		this.minDuration = origin.getMinDuration();
		this.maxDuration = origin.getMaxDuration();
		
		this.clusters = clusters;
		for (int i = 0; i < clusters.length; i++) {
			clusters[i].setName("Cluster #" + i);
			clusters[i].getRep().setName("Cluster #" + i);
		}
		
		this.rep = rep;
		rep.setName("Average for " + rep.weight + " iterations/threads of " + origin.name);
		
		this.metrics.setInclusiveDiffScore(0);
		this.metrics.setExclusiveDiffScore(0);
		
		this.originFileName = originFileName;
		this.originVoidDuplicate = (AbstractTraceNode)origin.voidDuplicate();
		
		if (origin != null && originFileName != null && originFileName.length() > 0) {
			try {
				FileOutputStream fileOut = new FileOutputStream(originFileName);
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
				out.writeObject(origin);
				out.close();
				fileOut.close();
			} catch (IOException e) {
				System.err.println("File not found when serializing origin for ClusterSetNode");
				e.printStackTrace();
			}
		}
		
		this.setDepth(origin.depth);
	}
	
	// Build new ClusterTreeNode from two existing ClusterTreeNode.
	public ClusterSetNode(ClusterSetNode node1, ClusterSetNode node2, AbstractTreeNode rep, Cluster[] clusters) {
		super(node1.getID(), node1.getName(), node1.getDepth(), node1.getCFGGraph(), node1.getAddrNode());
		this.originFileName = null;
		this.originVoidDuplicate = (AbstractTraceNode) node1.originVoidDuplicate.duplicate();
		this.weight = node1.weight + node2.weight;
		this.minDuration = TraceAnalysisUtils.computeWeightedAverage(node1.getMinDuration(), node1.getWeight(), node2.getMinDuration(), node2.getWeight());
		this.maxDuration = TraceAnalysisUtils.computeWeightedAverage(node1.getMaxDuration(), node1.getWeight(), node2.getMaxDuration(), node2.getWeight());
		
		this.clusters = clusters;
		for (int i = 0; i < clusters.length; i++) {
			clusters[i].setName("Cluster #" + i);
			clusters[i].getRep().setName("Cluster #" + i);
		}
		
		this.rep = rep;
		rep.setName("Average for " + rep.weight + " iterations/threads of " + node1.getName().substring(11));
		
		this.metrics = rep.metrics.duplicate();
		
		this.setDepth(node1.depth);
	}
	
	protected ClusterSetNode(ClusterSetNode other) {
		super(other);
		this.minDuration = other.minDuration;
		this.maxDuration = other.maxDuration;
		this.clusters = new Cluster[other.clusters.length];
		for (int i = 0; i < clusters.length; i++)
			clusters[i] = (Cluster) other.clusters[i].duplicate();
		this.rep = other.rep.duplicate();
		this.originFileName = other.originFileName;
		this.originVoidDuplicate = other.originVoidDuplicate;
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
		AbstractTraceNode origin = null;
			try {
				FileInputStream fileIn = new FileInputStream(this.originFileName);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				origin = (AbstractTraceNode) in.readObject();
				origin.setDepth(depth);
				in.close();
				fileIn.close();
			} catch (IOException e) {
				System.err.println("File not found when deserializing origin for ClusterSetNode");
				e.printStackTrace();
			} catch (ClassNotFoundException c) {
				System.err.println("class not found when deserializing origin for ClusterSetNode");
		        c.printStackTrace();
			}
		return origin;
	}
	
	public void setDepth(int depth) {
		super.setDepth(depth);
		originVoidDuplicate.setDepth(depth);
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
		return new ClusterSetNode(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		ClusterSetNode ret = new ClusterSetNode((AbstractTraceNode)originVoidDuplicate.voidDuplicate(), "", rep.voidDuplicate(), new Cluster[0]);
		ret.minDuration = 0;
		ret.maxDuration = 0;
		ret.name = this.name;
		return ret;
	}
	
	public void addLabel(int ID) {
		for (Cluster cluster : clusters) 
			cluster.addLabel(ID);
	}

	public String printLargeDiffNodes(int maxDepth, long durationCutoff, long totalDiff) {
		if (this.depth > maxDepth) return "";
		if (this.getDuration() < durationCutoff) return "";
		if (this.metrics.getInclusiveDiffScore() < totalDiff / TraceAnalysisUtils.diffCutoffDivider) return "";
		
		String ret = "C ";

		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		if (this.getTraceTime() != null)
			ret += " " + (getTraceTime().startTimeExclusive + getTraceTime().startTimeInclusive) / 2 / printDivisor + 
					" ~ " + (getTraceTime().endTimeInclusive + getTraceTime().endTimeExclusive) / 2 / printDivisor;
		ret += "  Duration = " + (minDuration + maxDuration) / 2 / printDivisor;
		
		String retChild = "";
		if (totalDiff > 0) retChild += diffRatioString(totalDiff);
		retChild += "\n";
		
		for (int i = 0; i < clusters.length; i++) 
			retChild += clusters[i].printLargeDiffNodes(maxDepth, durationCutoff, totalDiff);
		
		if (totalDiff < 0) {
			retChild += rep.printLargeDiffNodes(maxDepth, durationCutoff, rep.getDuration() * rep.weight * (rep.weight - 1));
			return ret + retChild;
		}
		else {
			retChild += rep.printLargeDiffNodes(maxDepth, durationCutoff, totalDiff);
			if (retChild.length() > 1) return ret + retChild;
			else return "";
		}
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		String ret = "CL";
		
		if (cfgGraph == null) ret += "       ";
		else ret += Long.toHexString(cfgGraph.vma) + " ";
		
		ret += "       ";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + minDuration / printDivisor + " ~ " + maxDuration / printDivisor;
		
		if (weight == 0) weight = rep.weight;
		
		if (metrics.getInclusiveDiffScore() != 0)
			ret += diffScoreString(weight);
		
		ret += '\n';
		
		for (int i = 0; i < clusters.length; i++) 
			ret += clusters[i].printLargeDiffNodes(maxDepth, durationCutoff, 0);
		
		//if (this.ID == 1598) maxDepth += 5;
		ret += rep.toString(maxDepth+1, durationCutoff, weight);
		
		//for (int i = 0; i < clusters.length; i++) 
		//	ret += clusters[i].toString(maxDepth+1, durationCutoff);
		
		return ret;
	}
}

