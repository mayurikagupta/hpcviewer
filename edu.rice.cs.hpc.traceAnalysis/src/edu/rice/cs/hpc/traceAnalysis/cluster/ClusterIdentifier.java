package edu.rice.cs.hpc.traceAnalysis.cluster;

import java.util.HashMap;

import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.FunctionTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IteratedLoop;
import edu.rice.cs.hpc.traceAnalysis.data.tree.Iteration;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ProfileNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTimeStruct;

public class ClusterIdentifier {
	static public final double minClusterDiff = 0.01;
	static public final double maxClusterDiff = 0.10;
	static public final double maxMinRatio = 3;
	
	static public int maxNumOfClusters(int numOfInstances) {
		// log2(numOfInstance) + 1
		return 31 - Integer.numberOfLeadingZeros(numOfInstances) + 1;
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
	
	// following four score computation functions are tested and they generated expecting output
	
	static private long computeRangeDiff(long min1, long max1, long min2, long max2) {
		if (max2 < min1) return min1 - max2;
		if (max1 < min2) return min2 - max1;
		return 0;
	}
	
	static private long computeTraceDiff(AbstractTraceNode trace1, AbstractTraceNode trace2) {
		HashMap<Integer, Integer> map1 = getOccurrenceMap(trace1);
		HashMap<Integer, Integer> map2 = getOccurrenceMap(trace2);
		
		int k1 = 0;
		int k2 = 0;
		
		long diff = 0;
		long gapDiffMin = 0;
		long gapDiffMax = 0;
		
		while (k1 < trace1.getNumOfChildren() && k2 < trace2.getNumOfChildren()) {
			// At the same child 
			if (trace1.getChild(k1).getID() == trace2.getChild(k2).getID()) {
				// Compute the difference between the gaps among (k1-1, k1) in node1 and (k2-1, k2) in node2.
				diff += computeRangeDiff(trace1.getMinGapDurationBeforeChild(k1) + gapDiffMin, trace1.getMaxGapDurationBeforeChild(k1) + gapDiffMax,
							trace2.getMinGapDurationBeforeChild(k2), trace2.getMaxGapDurationBeforeChild(k2));
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
				long diff1 = computeDiff(trace1.getChild(k1), trace2.getChild(k2));
				long diff2 = computeRangeDiff(trace1.getChild(k1).getMinDuration(), trace1.getChild(k1).getMaxDuration(),
						trace2.getChild(k2).getMinDuration(), trace2.getChild(k2).getMaxDuration());
				
				diff += Math.max(diff1, diff2);
							
				k1++;
				k2++;
			}
			// At different children
			else if (
					// Advance k2 if ---
					// the child in node1 occurs later in node 2
					(map2.containsKey(trace1.getChild(k1).getID()) && (map2.get(trace1.getChild(k1).getID()) > k2)) ||
					// OR the child in node2 has occurred before in node 1
					(map1.containsKey(trace2.getChild(k2).getID()) && (map1.get(trace2.getChild(k2).getID()) < k1)) 
					) {
				gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
				gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
				diff += trace2.getChild(k2).getDuration();
				k2++;
			} else { // Advance k1 in other cases 
				gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
				gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
				diff += trace1.getChild(k1).getDuration();
				k1++;
			}
		}
		
		while (k1 < trace1.getNumOfChildren()) {
			gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
			gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
			diff += trace1.getChild(k1).getDuration();
			k1++;
		}
		while (k2 < trace2.getNumOfChildren()) {
			gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
			gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
			diff += trace2.getChild(k2).getDuration();
			k2++;
		}
		
		// final gap
		gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
		gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
		gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
		gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
		
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
				diff += child1.getDuration();
		
		for (ProfileNode child2 : map2.values())
			// Children that only profile 2 has.
			if (!map1.containsKey(child2.getID()))
				diff += child2.getDuration();
		
		diff += computeRangeDiff(prof1.getMinExclusiveDuration(), prof1.getMaxExclusiveDuration(), prof2.getMinExclusiveDuration(), prof2.getMaxExclusiveDuration());

		return diff;
	}
	
	static private long computeClusterDiff(ClusterNode node1, ClusterNode node2) {
		int numCluster1 = node1.getNumOfClusters();
		int numCluster2 = node2.getNumOfClusters();
		
		AbstractTreeNode[] rep1 = new AbstractTreeNode[numCluster1];
		int[] numMember1 = new int[numCluster1];
		for (int i = 0; i < numCluster1; i++) {
			rep1[i] = node1.getCluster(i).getRep();
			numMember1[i] = node1.getCluster(i).getNumOfMembers() * node2.getNumInstance();
		}
		
		AbstractTreeNode[] rep2 = new AbstractTreeNode[numCluster2];
		int[] numMember2 = new int[numCluster2];
		for (int i = 0; i < numCluster2; i++) {
			rep2[i] = node2.getCluster(i).getRep();
			numMember2[i] = node2.getCluster(i).getNumOfMembers() * node1.getNumInstance();
		}
		
		long[][] diffScore = new long[numCluster1][numCluster2];
		double[][] diffRatio = new double[numCluster1][numCluster2];
		for (int i = 0; i < numCluster1; i++)
			for (int j = 0; j < numCluster2; j++) {
				diffScore[i][j] = computeDiff(rep1[i], rep2[j]);
				diffRatio[i][j] = (double)diffScore[i][j] / (double)(rep1[i].getDuration() + rep2[j].getDuration());
			}
		
		long totalDiff = 0;

		while (numCluster1 > 0 && numCluster2 > 0) {
			int idx1 = 0, idx2 = 0;
			for (int i = 0; i < numCluster1; i++)
				for (int j = 0; j < numCluster2; j++) 
					if (diffRatio[i][j] < diffRatio[idx1][idx2]) {
						idx1 = i;
						idx2 = j;
					}
			
			int numMatched = Math.min(numMember1[idx1], numMember2[idx2]);
			totalDiff += diffScore[idx1][idx2] * numMatched;
			
			numMember1[idx1] -= numMatched;
			numMember2[idx2] -= numMatched;
			
			// Move the last cluster to the current position
			if (numMember1[idx1] == 0) {
				numCluster1 --;
				rep1[idx1] = rep1[numCluster1];
				numMember1[idx1] = numMember1[numCluster1];
				for (int j = 0; j < numCluster2; j++) {
					diffScore[idx1][j] = diffScore[numCluster1][j];
					diffRatio[idx1][j] = diffRatio[numCluster1][j];
				}
			}
			
			if (numMember2[idx2] == 0) {
				numCluster2 --;
				rep2[idx2] = rep2[numCluster2];
				numMember2[idx2] = numMember2[numCluster2];
				for (int i = 0; i < numCluster1; i++) {
					diffScore[i][idx2] = diffScore[i][numCluster2];
					diffRatio[i][idx2] = diffRatio[i][numCluster2];
				}
			}
		}
		
		while (numCluster1 > 0) {
			numCluster1 --;
			totalDiff += rep1[numCluster1].getDuration() * numMember1[numCluster1];
		}
		
		while (numCluster2 > 0) {
			numCluster2 --;
			totalDiff += rep2[numCluster2].getDuration() * numMember2[numCluster2];
		}		

		totalDiff = totalDiff / (node1.getNumInstance() * node2.getNumInstance());
		
		return totalDiff;
	}
	
	static public long computeDiff(AbstractTreeNode node1, AbstractTreeNode node2) {
		if ((node1 instanceof FunctionTrace) && (node2 instanceof FunctionTrace))
			return computeTraceDiff((AbstractTraceNode)node1, (AbstractTraceNode)node2);
		else if ((node1 instanceof Iteration) && (node2 instanceof Iteration))
			return computeTraceDiff((AbstractTraceNode)node1, (AbstractTraceNode)node2);
		else if ((node1 instanceof ClusterNode) && (node2 instanceof ClusterNode))
			return computeClusterDiff((ClusterNode)node1, (ClusterNode)node2);
		//TODO IteratedLoopTrace
		else return computeProfileDiff(node1, node2);
	}
	
	/*
	 * Node merge functions.
	 */
	static private long computeWeightedAverage(long value1, int weight1, long value2, int weight2) {
		long total = value1 * weight1 + value2 * weight2;
		int divisor = weight1 + weight2;
		return (total + divisor / 2) / divisor;
	}
	
	static private TraceTimeStruct mergeTimeStruct(TraceTimeStruct time1, int weight1, TraceTimeStruct time2, int weight2) {
		TraceTimeStruct mergedTime = new TraceTimeStruct();
		
		mergedTime.setEndTimeExclusive(computeWeightedAverage(time1.getEndTimeExclusive(), weight1,
				time2.getEndTimeExclusive(), weight2));
		mergedTime.setEndTimeInclusive(computeWeightedAverage(time1.getEndTimeInclusive(), weight1,
				time2.getEndTimeInclusive(), weight2));
		mergedTime.setStartTimeExclusive(computeWeightedAverage(time1.getStartTimeExclusive(), weight1,
				time2.getStartTimeExclusive(), weight2));
		mergedTime.setStartTimeInclusive(computeWeightedAverage(time1.getStartTimeInclusive(), weight1,
				time2.getStartTimeInclusive(), weight2));
		
		return mergedTime;
	}
	
	static private AbstractTraceNode mergeTraceNode(AbstractTraceNode trace1, int weight1, AbstractTraceNode trace2, int weight2) {
		AbstractTraceNode mergedTrace = (AbstractTraceNode)trace1.duplicate();
		
		mergedTrace.setTime(mergeTimeStruct(trace1.getTime(), weight1, trace2.getTime(), weight2));
		mergedTrace.clearChildren();
		
		HashMap<Integer, Integer> map1 = getOccurrenceMap(trace1);
		HashMap<Integer, Integer> map2 = getOccurrenceMap(trace2);
		
		int k1 = 0;
		int k2 = 0;
		
		long startExclusive1 = trace1.getTime().getStartTimeExclusive();
		long startInclusive1 = trace1.getTime().getStartTimeInclusive();
		long startExclusive2 = trace2.getTime().getStartTimeExclusive();
		long startInclusive2 = trace2.getTime().getStartTimeInclusive();
		
		while (k1 < trace1.getNumOfChildren() && k2 < trace2.getNumOfChildren()) {
			// At the same child 
			if (trace1.getChild(k1).getID() == trace2.getChild(k2).getID()) {
				// Compute the difference between the gaps among (k1-1, k1) in node1 and (k2-1, k2) in node2.
				AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), weight1, trace2.getChild(k2), weight2);
				if (mergedChild instanceof AbstractTraceNode) 
					mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime());
				else 
					mergedTrace.addChild(mergedChild, mergeTimeStruct(trace1.getChildTime(k1), weight1, trace2.getChildTime(k2), weight2));
				
				startExclusive1 = trace1.getChildTime(k1).getEndTimeInclusive();
				startInclusive1 = trace1.getChildTime(k1).getEndTimeExclusive();
				startExclusive2 = trace2.getChildTime(k2).getEndTimeInclusive();
				startInclusive2 = trace2.getChildTime(k2).getEndTimeExclusive();
				k1++;
				k2++;
			}
			// At different children
			else if (
					// Advance k2 if the child in trace1 occurs later in trace2
					(map2.containsKey(trace1.getChild(k1).getID()) && (map2.get(trace1.getChild(k1).getID()) > k2)) ||
					// or if child in trace2 is not found in trace1
					!(map1.containsKey(trace2.getChild(k2).getID()))
					) {
				startExclusive1 += trace2.getChildTime(k2).getStartTimeExclusive() - startExclusive2;
				startInclusive1 += trace2.getChildTime(k2).getStartTimeInclusive() - startInclusive2;
				startExclusive1 = Math.min(startExclusive1, trace1.getChildTime(k1).getStartTimeExclusive());
				startInclusive1 = Math.min(startInclusive1, trace1.getChildTime(k1).getStartTimeInclusive());
				
				FunctionTrace dummeyTrace1 = new FunctionTrace(trace2.getChild(k2).getID(), trace2.getChild(k2).getName(), trace2.getChild(k2).getDepth());
				dummeyTrace1.getTime().setStartTimeExclusive(startExclusive1);
				dummeyTrace1.getTime().setStartTimeInclusive(startInclusive1);
				dummeyTrace1.getTime().setEndTimeInclusive(startExclusive1);
				dummeyTrace1.getTime().setEndTimeExclusive(startInclusive1);
				
				AbstractTreeNode mergedChild = mergeNode(dummeyTrace1, weight1, trace2.getChild(k2), weight2);
				if (mergedChild instanceof AbstractTraceNode) 
					mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime());
				else 
					mergedTrace.addChild(mergedChild, mergeTimeStruct(dummeyTrace1.getTime(), weight1, trace2.getChildTime(k2), weight2));
				
				startExclusive2 = trace2.getChildTime(k2).getEndTimeInclusive();
				startInclusive2 = trace2.getChildTime(k2).getEndTimeExclusive();
				k2++;
			}
			else if (
					// Advance k1 if the child in trace2 occurs later in trace1
					(map1.containsKey(trace2.getChild(k2).getID()) && (map1.get(trace2.getChild(k2).getID()) > k1)) ||
					// or if child in trace1 is not found in trace2
					!(map2.containsKey(trace1.getChild(k1).getID()))
					) 
			{
				startExclusive2 += trace1.getChildTime(k1).getStartTimeExclusive() - startExclusive1;
				startInclusive2 += trace1.getChildTime(k1).getStartTimeInclusive() - startInclusive1;
				startExclusive2 = Math.min(startExclusive2, trace2.getChildTime(k2).getStartTimeExclusive());
				startInclusive2 = Math.min(startInclusive2, trace2.getChildTime(k2).getStartTimeInclusive());
				
				FunctionTrace dummeyTrace2 = new FunctionTrace(trace1.getChild(k1).getID(), trace1.getChild(k1).getName(), trace1.getChild(k1).getDepth());
				dummeyTrace2.getTime().setStartTimeExclusive(startExclusive2);
				dummeyTrace2.getTime().setStartTimeInclusive(startInclusive2);
				dummeyTrace2.getTime().setEndTimeInclusive(startExclusive2);
				dummeyTrace2.getTime().setEndTimeExclusive(startInclusive2);
				
				AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), weight1, dummeyTrace2, weight2);
				if (mergedChild instanceof AbstractTraceNode) 
					mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime());
				else 
					mergedTrace.addChild(mergedChild, mergeTimeStruct(trace1.getChildTime(k1), weight1, dummeyTrace2.getTime(), weight2));
				
				startExclusive1 = trace1.getChildTime(k1).getEndTimeInclusive();
				startInclusive1 = trace1.getChildTime(k1).getEndTimeExclusive();
				k1++;
			}
			else {
				System.err.println("ERROR while merging trace nodes");
				return null;
			}
		}
		
		while (k1 < trace1.getNumOfChildren()) {
			startExclusive2 += trace1.getChildTime(k1).getStartTimeExclusive() - startExclusive1;
			startInclusive2 += trace1.getChildTime(k1).getStartTimeInclusive() - startInclusive1;
			startExclusive2 = Math.min(startExclusive2, trace2.getTime().getEndTimeInclusive());
			startInclusive2 = Math.min(startInclusive2, trace2.getTime().getEndTimeExclusive());
			
			FunctionTrace dummeyTrace2 = new FunctionTrace(trace1.getChild(k1).getID(), trace1.getChild(k1).getName(), trace1.getChild(k1).getDepth());
			dummeyTrace2.getTime().setStartTimeExclusive(startExclusive2);
			dummeyTrace2.getTime().setStartTimeInclusive(startInclusive2);
			dummeyTrace2.getTime().setEndTimeInclusive(startExclusive2);
			dummeyTrace2.getTime().setEndTimeExclusive(startInclusive2);
			
			AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), weight1, dummeyTrace2, weight2);
			if (mergedChild instanceof AbstractTraceNode) 
				mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime());
			else 
				mergedTrace.addChild(mergedChild, mergeTimeStruct(trace1.getChildTime(k1), weight1, dummeyTrace2.getTime(), weight2));
			
			startExclusive1 = trace1.getChildTime(k1).getEndTimeInclusive();
			startInclusive1 = trace1.getChildTime(k1).getEndTimeExclusive();
			k1++;
		}
		
		while (k2 < trace2.getNumOfChildren()) {
			startExclusive1 += trace2.getChildTime(k2).getStartTimeExclusive() - startExclusive2;
			startInclusive1 += trace2.getChildTime(k2).getStartTimeInclusive() - startInclusive2;
			startExclusive1 = Math.min(startExclusive1, trace1.getTime().getEndTimeInclusive());
			startInclusive1 = Math.min(startInclusive1, trace1.getTime().getEndTimeExclusive());
			
			FunctionTrace dummeyTrace1 = new FunctionTrace(trace2.getChild(k2).getID(), trace2.getChild(k2).getName(), trace2.getChild(k2).getDepth());
			dummeyTrace1.getTime().setStartTimeExclusive(startExclusive1);
			dummeyTrace1.getTime().setStartTimeInclusive(startInclusive1);
			dummeyTrace1.getTime().setEndTimeInclusive(startExclusive1);
			dummeyTrace1.getTime().setEndTimeExclusive(startInclusive1);
			
			AbstractTreeNode mergedChild = mergeNode(dummeyTrace1, weight1, trace2.getChild(k2), weight2);
			if (mergedChild instanceof AbstractTraceNode) 
				mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime());
			else 
				mergedTrace.addChild(mergedChild, mergeTimeStruct(dummeyTrace1.getTime(), weight1, trace2.getChildTime(k2), weight2));
			
			startExclusive2 = trace2.getChildTime(k2).getEndTimeInclusive();
			startInclusive2 = trace2.getChildTime(k2).getEndTimeExclusive();
			k2++;
		}
		
		return mergedTrace;
	}
	
	static private ProfileNode mergeProfileNode(AbstractTreeNode node1, int weight1, AbstractTreeNode node2, int weight2) {
		ProfileNode prof1 = ProfileNode.toProfile(node1);
		ProfileNode prof2 = ProfileNode.toProfile(node2);

		prof1.stretch(weight1, 1);
		prof2.stretch(weight2, 1);
		
		prof1.merge(prof2);
		
		prof1.stretch(1, weight1 + weight2);
			
		return prof1;
	}
	
	static private AbstractTreeNode mergeClusterNode(ClusterNode node1, int weight1, ClusterNode node2, int weight2) {
		Cluster[] cluster = new Cluster[node1.getNumOfClusters() + node2.getNumOfClusters()];
		
		for (int i = 0; i < node1.getNumOfClusters(); i++)
			cluster[i] = node1.getCluster(i);
		for (int i = 0; i < node2.getNumOfClusters(); i++)
			cluster[i+node1.getNumOfClusters()] = node2.getCluster(i);
		
		int numMembers = 0;
		for (Cluster c : cluster) 
			numMembers += c.getNumOfMembers();
		
		cluster = mergeCluster(cluster, maxNumOfClusters(numMembers));
		
		if (cluster != null) {
			long minDuration = computeWeightedAverage(node1.getMinDuration(), weight1, node2.getMinDuration(), weight2);
			long maxDuration = computeWeightedAverage(node1.getMaxDuration(), weight1, node2.getMaxDuration(), weight2);
			
			return new ClusterNode(node1, node2, cluster, minDuration, maxDuration);
		}
		else
			return mergeProfileNode(node1, weight1, node2, weight2);
	}
	
	static private AbstractTreeNode mergeNode(AbstractTreeNode node1, int weight1, AbstractTreeNode node2, int weight2) {
		/*if (weight1 > weight2) return node1;
		if (weight2 > weight1) return node2;
		
		if (random.nextBoolean()) return node1;
		else return node2;*/
		
		if ((node1 instanceof FunctionTrace) && (node2 instanceof FunctionTrace))
			return mergeTraceNode((AbstractTraceNode)node1, weight1, (AbstractTraceNode)node2, weight2);
		else if ((node1 instanceof Iteration) && (node2 instanceof Iteration))
			return mergeTraceNode((AbstractTraceNode)node1, weight1, (AbstractTraceNode)node2, weight2);
		else if ((node1 instanceof ClusterNode) && (node2 instanceof ClusterNode))
			return mergeClusterNode((ClusterNode)node1, weight1, (ClusterNode)node2, weight2);
		//TODO IteratedLoopTrace
		else return mergeProfileNode(node1, weight1, node2, weight2);
		
	}
	
	static private Cluster[] mergeCluster(Cluster[] cluster, int maxNumCluster) {
		int numCluster = cluster.length;
		double[][] diff = new double[numCluster][numCluster];
		
		double minDiff = 1;
		double maxDiff = 0;
		int idx1 = 1, idx2 = 2;
		
		for (int i = 0; i < numCluster; i++)
			for (int j = i+1; j < numCluster; j++) {
				diff[i][j] = (double)computeDiff(cluster[i].getRep(), cluster[j].getRep())
					/ (double)(cluster[i].getRep().getDuration() + cluster[j].getRep().getDuration());
				maxDiff = Math.max(maxDiff, diff[i][j]);
				if (diff[i][j] < minDiff) {
					minDiff = diff[i][j];
					idx1 = i;
					idx2 = j;
				}
			}
/*
if (cluster[0].getRep().getID() == 69617) {
	System.out.println("*****************************************************");
	for (int i = 0; i < numCluster; i++) {
		System.out.println(cluster[i].getMembers());
		System.out.print(cluster[i].getRep().print(0, 0));
		for (int j = i+1; j < numCluster; j++)
			System.out.print("\t" + diff[i][j]);
		System.out.println();
	}
	System.out.println("minDiff = " + minDiff + " @ (" + idx1 + ", " + idx2 + ")");
}
*/
		/**
		 * Merge the closest two clusters if
		 * 1. minDiff is too small;
		 * 2. minDiff is too small compared to maxDiff (and not greater than maxClusterDiff);
		 * 3. minDiff is less than maxClusterDiff and numCluster exceeds the threshold.
		 */
		while (minDiff <= minClusterDiff 
				|| minDiff <= Math.min(maxDiff/maxMinRatio, maxClusterDiff)
				|| (minDiff <= maxClusterDiff && numCluster > maxNumCluster)) {
			AbstractTreeNode node = mergeNode(cluster[idx1].getRep(), cluster[idx1].getNumOfMembers(), 
					  cluster[idx2].getRep(), cluster[idx2].getNumOfMembers());
			if (node == null) {
				System.out.println(cluster[idx1].getRep().print(cluster[idx1].getRep().getDepth()+1, 0));
				System.out.println(cluster[idx2].getRep().print(cluster[idx2].getRep().getDepth()+1, 0));
			}
			cluster[idx1] = new Cluster(node, cluster[idx1].getMembers());
			cluster[idx1].addMembers(cluster[idx2].getMembers());
			
			// Remove the cluster at idx2 and update diff matrix.
			for (int i = idx2+1; i < numCluster; i++) {
				cluster[i-1] = cluster[i];
				for (int j = 0; j < idx2; j++)
					diff[j][i-1] = diff[j][i];
				for (int j = idx2+1; j < i; j++)
					diff[j-1][i-1] = diff[j][i];
			}
			numCluster--;
			
			// recompute diff values for the merged cluster.
			for (int i = 0; i < idx1; i++)
				diff[i][idx1] = (double)computeDiff(cluster[i].getRep(), cluster[idx1].getRep())
						/ (double)(cluster[i].getRep().getDuration() + cluster[idx1].getRep().getDuration());
			for (int i = idx1+1; i < numCluster; i++)
				diff[idx1][i] = (double)computeDiff(cluster[idx1].getRep(), cluster[i].getRep())
						/ (double)(cluster[idx1].getRep().getDuration() + cluster[i].getRep().getDuration());
			
			minDiff = 1;
			maxDiff = 0;
			idx1 = 1;
			idx2 = 2;
			for (int i = 0; i < numCluster; i++)
				for (int j = i+1; j < numCluster; j++) {
					maxDiff = Math.max(maxDiff, diff[i][j]);
					if (diff[i][j] < minDiff) {
						minDiff = diff[i][j];
						idx1 = i;
						idx2 = j;
					}
				}	
/*
if (cluster[0].getRep().getID() == 69617) {
	System.out.println("*****************************************************");
	for (int i = 0; i < numCluster; i++) {
		System.out.println(cluster[i].getMembers());
		System.out.print(cluster[i].getRep().print(0, 0));
		for (int j = i+1; j < numCluster; j++)
			System.out.print("\t" + diff[i][j]);
		System.out.println();
	}
	System.out.println("minDiff = " + minDiff + " @ (" + idx1 + ", " + idx2 + ")");
}	
*/
		}
		
		if (numCluster <= maxNumCluster) {
			Cluster[] ret = new Cluster[numCluster];
			for (int i = 0; i < numCluster; i++)
				ret[i] = cluster[i];
			return ret;
		}
		else return null;
	}
	
	static private void labelCluster(AbstractTreeNode node, int ID) {
		if (node instanceof ClusterNode)
			((ClusterNode) node).addLabel(ID);
		else if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++)
				labelCluster(trace.getChild(i), ID);
		}
	}
	
	// fully tested.
	static private Cluster[] findCluster(AbstractTraceNode loop, int begin, int end, int maxNumCluster) {
		if (begin == end) {
			labelCluster(loop.getChild(begin), begin);
			
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
		
		return mergeCluster(cluster, maxNumCluster);
	}
	
	static public ClusterNode findCluster(AbstractTraceNode loop) {
		if (loop.getID() == 0) testDiff(loop);
		Cluster[] cluster = findCluster(loop, 0, loop.getNumOfChildren()-1, maxNumOfClusters(loop.getNumOfChildren()));
		if (cluster == null) return null;
		else return new ClusterNode(loop, cluster);
	}
	
	static public void testDiff(AbstractTreeNode node) {
		if (node.getID() == 0) {
		//if (node instanceof IteratedLoop) {
			AbstractTraceNode loop = (AbstractTraceNode)node;
			//if (loop.getID() == 69617 || loop.getID() == 23299) {
				System.out.println(loop.getName()+"("+loop.getID()+")");
				System.out.print(loop.print(loop.getDepth(), 0));
				for (int i = 0; i < loop.getNumOfChildren(); i++) {
					for (int j = 0; j < loop.getNumOfChildren(); j++) {
						long diff = computeDiff(loop.getChild(i), loop.getChild(j));
						diff = diff * 10000 / (loop.getChild(i).getDuration() + loop.getChild(j).getDuration());
						System.out.print("\t" + diff/100 + "." + diff%100/10 + diff%10);
					}
					System.out.println();
				}
				System.out.println();
			//}
			//testDiff(loop.getChild(loop.getNumOfChildren()-1));
		}
		/*else if (node instanceof AbstractTraceNode)
			for (int i = 0; i < ((AbstractTraceNode)node).getNumOfChildren(); i++) 
			testDiff(((AbstractTraceNode)node).getChild(i));*/
	}
}
