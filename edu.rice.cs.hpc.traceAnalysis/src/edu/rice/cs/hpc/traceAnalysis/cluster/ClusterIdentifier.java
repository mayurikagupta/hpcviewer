package edu.rice.cs.hpc.traceAnalysis.cluster;

import java.util.HashMap;
import java.util.Random;

import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.FunctionTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IteratedLoop;
import edu.rice.cs.hpc.traceAnalysis.data.tree.Iteration;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ProfileNode;

public class ClusterIdentifier {
	static public final double minClusterDiff = 0.01;
	static public final double maxClusterDiff = 0.10;
	
	static private final Random random = new Random(15);
	
	static public int maxNumOfClusters(int numOfInstances) {
		return (int)Math.log(numOfInstances) + 1;
	}
	
	static private HashMap<Integer, Integer> getOccurrenceMap(AbstractTraceNode node) {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (int i = 0; i < node.getNumOfChildren(); i++) 
			map.put(node.getChild(i).getID(), i);
		return map;
	}
	
	/*
	 * Difference score computation functions.
	 */
	
	static private long computeRangeDiff(long min1, long max1, long min2, long max2) {
		if (max2 < min1) return min1 - max2;
		if (max1 < min2) return min2 - max1;
		return 0;
	}
	
	static private long computeTraceDiff(AbstractTraceNode node1, AbstractTraceNode node2) {
		HashMap<Integer, Integer> map1 = getOccurrenceMap(node1);
		HashMap<Integer, Integer> map2 = getOccurrenceMap(node2);
		
		int k1 = 0;
		int k2 = 0;
		
		long diff = 0;
		long gapDiffMin = 0;
		long gapDiffMax = 0;
		
		while (k1 < node1.getNumOfChildren() && k2 < node2.getNumOfChildren()) {
			// At the same child 
			if (node1.getChild(k1).getID() == node2.getChild(k2).getID()) {
				// Compute the difference between the gaps among (k1-1, k1) in node1 and (k2-1, k2) in node2.
				diff += computeRangeDiff(node1.getMinGapDurationBeforeChild(k1) + gapDiffMin, node1.getMaxGapDurationBeforeChild(k1) + gapDiffMax,
							node2.getMinGapDurationBeforeChild(k2), node2.getMaxGapDurationBeforeChild(k2));
				gapDiffMin = 0;
				gapDiffMax = 0;
				// Compute the different between two children.
				
				/**
				// When both nodes are no less than detectionCutoff, explore them in detail.
				if (node1.getChild(k1).getMinDuration() >= detectionCutoff && node2.getChild(k2).getMinDuration() >= detectionCutoff)
					diff += computeDiff(node1.getChild(k1), node2.getChild(k2));
				// If not, use their duration to compute difference
				else
					diff += computeRangeDiff(node1.getChild(k1).getMinDuration(), node1.getChild(k1).getMaxDuration(),
							node2.getChild(k2).getMinDuration(), node2.getChild(k2).getMaxDuration());
							*/
				long diff1 = computeDiff(node1.getChild(k1), node2.getChild(k2));
				long diff2 = computeRangeDiff(node1.getChild(k1).getMinDuration(), node1.getChild(k1).getMaxDuration(),
						node2.getChild(k2).getMinDuration(), node2.getChild(k2).getMaxDuration());
				
				diff += Math.max(diff1, diff2);
							
				k1++;
				k2++;
			}
			// At different children
			else if (
					// Advance k2 if ---
					// the child in node1 occurs later in node 2
					(map2.containsKey(node1.getChild(k1).getID()) && (map2.get(node1.getChild(k1).getID()) > k2)) ||
					// OR the child in node2 has occurred before in node 1
					(map1.containsKey(node2.getChild(k2).getID()) && (map1.get(node2.getChild(k2).getID()) < k1)) 
					) {
				gapDiffMin -= node2.getMaxGapDurationBeforeChild(k2);
				gapDiffMax -= node2.getMinGapDurationBeforeChild(k2);
				diff += node2.getChild(k2).getMinDuration();
				k2++;
			} else { // Advance k1 in other cases 
				gapDiffMin += node1.getMinGapDurationBeforeChild(k1);
				gapDiffMax += node1.getMaxGapDurationBeforeChild(k1);
				diff += node1.getChild(k1).getMinDuration();
				k1++;
			}
		}
		
		while (k1 < node1.getNumOfChildren()) {
			gapDiffMin += node1.getMinGapDurationBeforeChild(k1);
			gapDiffMax += node1.getMaxGapDurationBeforeChild(k1);
			diff += node1.getChild(k1).getMinDuration();
			k1++;
		}
		while (k2 < node2.getNumOfChildren()) {
			gapDiffMin -= node2.getMaxGapDurationBeforeChild(k2);
			gapDiffMax -= node2.getMinGapDurationBeforeChild(k2);
			diff += node2.getChild(k2).getMinDuration();
			k2++;
		}
		
		// final gap
		gapDiffMin += node1.getMinGapDurationBeforeChild(k1);
		gapDiffMax += node1.getMaxGapDurationBeforeChild(k1);
		gapDiffMin -= node2.getMaxGapDurationBeforeChild(k2);
		gapDiffMax -= node2.getMinGapDurationBeforeChild(k2);
		
		diff += computeRangeDiff(gapDiffMin, gapDiffMax, 0, 0);
		
		return diff;
	}

	static private long computeProfileDiff(AbstractTreeNode node1, AbstractTreeNode node2) {
		long diff = 0;
		ProfileNode prof1 = ProfileNode.toProfile(node1);
		ProfileNode prof2 = ProfileNode.toProfile(node2);
		
		HashMap<Integer, ProfileNode> map1 = prof1.getChildMap();
		HashMap<Integer, ProfileNode> map2 = prof2.getChildMap();
		
		for (ProfileNode child1 : map1.values())
			// Children that both profile have.
			if (map2.containsKey(child1.getID())) {
				ProfileNode child2 = map2.get(child1.getID());
				/**
				if (child1.getMinDuration() >= detectionCutoff && child2.getMinDuration() >= detectionCutoff)
					diff += computeProfileDiff(child1, child2);
				else
					diff += computeRangeDiff(child1.getMinDuration(), child1.getMaxDuration(), child2.getMinDuration(), child2.getMaxDuration());
				**/
				long diff1 = computeProfileDiff(child1, child2);
				long diff2 = computeRangeDiff(child1.getMinDuration(), child1.getMaxDuration(), child2.getMinDuration(), child2.getMaxDuration());
				diff += Math.max(diff1, diff2);
			}
			// Children that only profile 1 has.
			else
				diff += child1.getMinDuration();
		
		for (ProfileNode child2 : map2.values())
			// Children that only profile 2 has.
			if (!map1.containsKey(child2.getID()))
				diff += child2.getMinDuration();
		
		diff += computeRangeDiff(prof1.getMinExclusiveDuration(), prof1.getMaxExclusiveDuration(), prof2.getMinExclusiveDuration(), prof2.getMaxExclusiveDuration());

		return diff;
	}
	
	static private long computeDiff(AbstractTreeNode node1, AbstractTreeNode node2) {
		if ((node1 instanceof FunctionTrace) && (node2 instanceof FunctionTrace))
			return computeTraceDiff((AbstractTraceNode)node1, (AbstractTraceNode)node2);
		else if ((node1 instanceof Iteration) && (node2 instanceof Iteration))
			return computeTraceDiff((AbstractTraceNode)node1, (AbstractTraceNode)node2);
		//else if ((node1 instanceof IteratedLoopTrace) && (node2 instanceof IteratedLoopTrace))
		//	return 0;
		//TODO IteratedLoopTrace
		//TODO ClusterNode
		else return computeProfileDiff(node1, node2);
	}
	
	/*
	 * Node merge functions.
	 */
	static private AbstractTreeNode mergeNode(AbstractTreeNode mainNode, int mainWeight, AbstractTreeNode sideNode, int sideWeight) {
		if (mainWeight > sideWeight) return mainNode;
		if (sideWeight > mainWeight) return sideNode;
		
		if (random.nextBoolean()) return mainNode;
		else return sideNode;
	}
	
	static public Cluster[] findCluster(AbstractTraceNode loop, int begin, int end, int maxNumCluster) {
		if (begin == end) {
			Cluster[] cluster = new Cluster[1];
			cluster[0] = new Cluster(loop.getChild(begin), begin);
			return cluster;
		}
		
		int mid = (begin+end)/2;
		Cluster[] cluster1 = findCluster(loop, begin, mid, maxNumCluster);
		Cluster[] cluster2 = findCluster(loop, mid+1, end, maxNumCluster);
		
		if (cluster1 == null || cluster2 == null) return null;
		
		Cluster[] cluster = new Cluster[cluster1.length + cluster2.length];
		for (int i = 0; i < cluster1.length; i++)
			cluster[i] = cluster1[i];
		for (int i = 0; i < cluster2.length; i++)
			cluster[i+cluster1.length] = cluster2[i];
		
		int numCluster = cluster.length;
		double[][] diff = new double[numCluster][numCluster];
		
		double minDiff = 1;
		double maxDiff = 0;
		int idx1 = 1, idx2 = 2;
		
		for (int i = 0; i < numCluster; i++)
			for (int j = i+1; j < numCluster; j++) {
				diff[i][j] = (double)computeDiff(cluster[i].getRep(), cluster[j].getRep())
					/ (double)(cluster[i].getRep().getMinDuration() + cluster[j].getRep().getMinDuration());
				maxDiff = Math.max(maxDiff, diff[i][j]);
				if (diff[i][j] < minDiff) {
					minDiff = diff[i][j];
					idx1 = i;
					idx2 = j;
				}
			}
		
		
		/**
		 * Merge the closest two clusters if
		 * 1. minDiff is too small;
		 * 2. minDiff is too small compared to maxDiff (and not greater than maxClusterDiff);
		 * 3. minDiff is less than maxClusterDiff and numCluster exceeds the threshold.
		 */
		while (minDiff <= minClusterDiff 
				|| minDiff <= Math.min(maxDiff/3, maxClusterDiff)
				|| (minDiff <= maxClusterDiff && numCluster > maxNumCluster)) {
			AbstractTreeNode node = mergeNode(cluster[idx1].getRep(), cluster[idx1].getNumOfMembers(), 
					  cluster[idx2].getRep(), cluster[idx2].getNumOfMembers());
			cluster[idx1] = new Cluster(node, cluster[idx1].getMembers());
			cluster[idx1].addMembers(cluster[idx2].getMembers());
			
			for (int i = idx2+1; i < numCluster; i++)
				cluster[i-1] = cluster[i];
			numCluster--;
			
			minDiff = 1;
			maxDiff = 0;
			idx1 = 1;
			idx2 = 2;
			for (int i = 0; i < numCluster; i++)
				for (int j = i+1; j < numCluster; j++) {
					diff[i][j] = (double)computeDiff(cluster[i].getRep(), cluster[j].getRep())
						/ (double)(cluster[i].getRep().getMinDuration() + cluster[j].getRep().getMinDuration());
					maxDiff = Math.max(maxDiff, diff[i][j]);
					if (diff[i][j] < minDiff) {
						minDiff = diff[i][j];
						idx1 = i;
						idx2 = j;
					}
				}
		}
		
		if (numCluster <= maxNumCluster) {
			Cluster[] ret = new Cluster[numCluster];
			for (int i = 0; i < numCluster; i++)
				ret[i] = cluster[i];
			return ret;
		}
		else return null;
	}
	
	static public ClusterNode findCluster(AbstractTraceNode loop) {
		Cluster[] cluster = findCluster(loop, 0, loop.getNumOfChildren()-1, maxNumOfClusters(loop.getNumOfChildren()));
		if (cluster == null) return null;
		else return new ClusterNode(loop, cluster);
	}
	
	static public void testDiff(AbstractTreeNode node) {
		if (node instanceof IteratedLoop) {
			IteratedLoop loop = (IteratedLoop)node;
			if (loop.getID() == 69617 || loop.getID() == 23299) {
				System.out.println(loop.getName()+"("+loop.getID()+")");
				System.out.print(loop.print(loop.getDepth(), 0));
				for (int i = 0; i < loop.getNumOfChildren(); i++) {
					for (int j = 0; j < loop.getNumOfChildren(); j++) {
						long diff = computeDiff(loop.getChild(i), loop.getChild(j));
						diff = diff * 10000 / (loop.getChild(i).getMinDuration() + loop.getChild(j).getMinDuration());
					System.out.print("\t" + diff/100 + "." + diff%100/10 + diff%10);
					}
					System.out.println();
				}
				System.out.println();
			}
			testDiff(loop.getChild(loop.getNumOfChildren()-1));
		}
		else if (node instanceof AbstractTraceNode)
			for (int i = 0; i < ((AbstractTraceNode)node).getNumOfChildren(); i++) 
			testDiff(((AbstractTraceNode)node).getChild(i));
	}
}
