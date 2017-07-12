package edu.rice.cs.hpc.traceAnalysis.cluster;

import java.util.HashMap;

import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.Cluster;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.FunctionTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IteratedLoopTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IterationTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ProfileNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTimeStruct;

public class ClusterIdentifier {
	static public final double minClusterDiff = 0.01;
	static public final double maxClusterDiff = 0.10;
	static public final double maxMinRatio = 3;
	
	static public int maxNumOfClusters(int numOfInstances) {
		return 31 - Integer.numberOfLeadingZeros(numOfInstances) + 1; // which is log2(numOfInstance) + 1
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

	static private long computeTraceDiff(AbstractTraceNode trace1, AbstractTraceNode trace2, boolean debug) {
		int k1 = 0;
		int k2 = 0;
		
		long diff = 0;
		long gapDiffMin = 0;
		long gapDiffMax = 0;
		
		while (k1 < trace1.getNumOfChildren() || k2 < trace2.getNumOfChildren()) {
			// At the same child 
			if (k1 < trace1.getNumOfChildren() && k2 < trace2.getNumOfChildren()
					&& trace1.getChild(k1).getID() == trace2.getChild(k2).getID()) {
				// Compute the difference between the gaps among (k1-1, k1) in node1 and (k2-1, k2) in node2.
				diff += computeRangeDiff(trace1.getMinGapDurationBeforeChild(k1) + gapDiffMin, trace1.getMaxGapDurationBeforeChild(k1) + gapDiffMax,
							trace2.getMinGapDurationBeforeChild(k2), trace2.getMaxGapDurationBeforeChild(k2));
				gapDiffMin = 0;
				gapDiffMax = 0;
				
				diff += computeDiff(trace1.getChild(k1), trace2.getChild(k2), debug);
							
				k1++;
				k2++;
			}
			else {
				if (trace1.cfgNode == null || trace2.cfgNode == null)
					return computeProfileDiff(trace1, trace2);
				// Increase k1 if
				if (k1 < trace1.getNumOfChildren() && 
				      (k2 == trace2.getNumOfChildren() || // k2 points to the end
					    (trace1.cfgNode.getChildIndex(trace1.getChildCFGNode(k1)) // k1 is prior of k2 in the CFG
						< trace2.cfgNode.getChildIndex(trace2.getChildCFGNode(k2))) || 
							((trace1.cfgNode.getChildIndex(trace1.getChildCFGNode(k1)) // k1 takes the same location as k2 in the CFG but its CCT ID is smaller
							 == trace2.cfgNode.getChildIndex(trace2.getChildCFGNode(k2)))
							 && (trace1.getID() < trace2.getID()))
					   )) {
					gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
					gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
					diff += trace1.getChild(k1).getDuration();
					k1++;
				}
				else {
					gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
					gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
					diff += trace2.getChild(k2).getDuration();
					k2++;
				}
			}
		}
		
		// final gap
		gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
		gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
		gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
		gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
		
		diff += computeRangeDiff(gapDiffMin, gapDiffMax, 0, 0);
		
		diff = Math.max(diff, computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
				trace2.getMinDuration(), trace2.getMaxDuration()));
		
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
	
	static private long computeClusterDiff(ClusterTreeNode node1, ClusterTreeNode node2, boolean debug) {
		int numCluster1 = node1.getNumOfClusters();
		int numCluster2 = node2.getNumOfClusters();
		
		AbstractTreeNode[] rep1 = new AbstractTreeNode[numCluster1];
		int[] numMember1 = new int[numCluster1];
		for (int i = 0; i < numCluster1; i++) {
			rep1[i] = node1.getCluster(i).getRep();
			numMember1[i] = node1.getCluster(i).getWeight() * node2.getWeight();
		}
		
		AbstractTreeNode[] rep2 = new AbstractTreeNode[numCluster2];
		int[] numMember2 = new int[numCluster2];
		for (int i = 0; i < numCluster2; i++) {
			rep2[i] = node2.getCluster(i).getRep();
			numMember2[i] = node2.getCluster(i).getWeight() * node1.getWeight();
		}
		
		long[][] diffScore = new long[numCluster1][numCluster2];
		double[][] diffRatio = new double[numCluster1][numCluster2];
		for (int i = 0; i < numCluster1; i++)
			for (int j = 0; j < numCluster2; j++) {
				diffScore[i][j] = computeDiff(rep1[i], rep2[j], debug);
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

		totalDiff = totalDiff / (node1.getWeight() * node2.getWeight());
		
		return totalDiff;
	}
	
	static public long computeDiff(AbstractTreeNode node1, AbstractTreeNode node2, boolean debug) {
		long diff = 0;
		if ((node1 instanceof FunctionTrace) && (node2 instanceof FunctionTrace))
			diff = computeTraceDiff((AbstractTraceNode)node1, (AbstractTraceNode)node2, debug);
		else if ((node1 instanceof IterationTrace) && (node2 instanceof IterationTrace))
			diff = computeTraceDiff((AbstractTraceNode)node1, (AbstractTraceNode)node2, debug);
		else if ((node1 instanceof ClusterTreeNode) && (node2 instanceof ClusterTreeNode))
			diff = computeClusterDiff((ClusterTreeNode)node1, (ClusterTreeNode)node2, debug);
		//TODO IteratedLoopTrace
		//TODO Trace/Profile with Cluster
		else 
			diff = computeProfileDiff(node1, node2);
		
		if (debug && diff > 0) {
			String str = "";
			for (int i = 0; i < node1.getDepth(); i++) str += "  ";
			System.out.println(str + node1.getID() + " vs. " + node2.getID() + " = " + diff);
		}
		return diff;
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
	
	static private AbstractTreeNode mergeTraceNode(AbstractTraceNode trace1, int weight1, AbstractTraceNode trace2, int weight2) {
		AbstractTraceNode mergedTrace = (AbstractTraceNode)trace1.duplicate();
		
		int w1 = trace1.getWeight();
		int w2 = trace2.getWeight();
		mergedTrace.setTime(mergeTimeStruct(trace1.getTime(), w1, trace2.getTime(), w2));
		mergedTrace.setWeight(w1 + w2);
		mergedTrace.clearChildren();
	
		int k1 = 0;
		int k2 = 0;
		
		long inclusiveDiff = trace1.getInclusiveDiffScore() + trace2.getInclusiveDiffScore();
		long gapDiffMin = 0;
		long gapDiffMax = 0;
		
		long startExclusive1 = trace1.getTime().getStartTimeExclusive();
		long startInclusive1 = trace1.getTime().getStartTimeInclusive();
		long startExclusive2 = trace2.getTime().getStartTimeExclusive();
		long startInclusive2 = trace2.getTime().getStartTimeInclusive();
		
		while (k1 < trace1.getNumOfChildren() || k2 < trace2.getNumOfChildren()) {
			// At the same child 
			if (k1 < trace1.getNumOfChildren() && k2 < trace2.getNumOfChildren()
					&& trace1.getChild(k1).getID() == trace2.getChild(k2).getID()) {	
				// Merge corresponding node
				AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), w1, trace2.getChild(k2), w2);
				if (mergedChild instanceof AbstractTraceNode) 
					mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime(), trace1.getChildCFGNode(k1));
				else 
					mergedTrace.addChild(mergedChild, mergeTimeStruct(trace1.getChildTime(k1), w1, trace2.getChildTime(k2), w2), trace1.getChildCFGNode(k1));
				
				startExclusive1 = trace1.getChildTime(k1).getEndTimeInclusive();
				startInclusive1 = trace1.getChildTime(k1).getEndTimeExclusive();
				startExclusive2 = trace2.getChildTime(k2).getEndTimeInclusive();
				startInclusive2 = trace2.getChildTime(k2).getEndTimeExclusive();
				
				// Compute the difference between the gaps among (k1-1, k1) in node1 and (k2-1, k2) in node2.
				long diff = computeRangeDiff(trace1.getMinGapDurationBeforeChild(k1) + gapDiffMin, trace1.getMaxGapDurationBeforeChild(k1) + gapDiffMax,
						trace2.getMinGapDurationBeforeChild(k2), trace2.getMaxGapDurationBeforeChild(k2));
				gapDiffMin = 0;
				gapDiffMax = 0;
						
				// The inclusive diff score of merged child already has the inclusive diff score of k1 and k2. Deduct them to avoid duplicates.
				inclusiveDiff += diff * w1 * w2;
				inclusiveDiff += mergedChild.getInclusiveDiffScore() - trace1.getChild(k1).getInclusiveDiffScore() 
						- trace2.getChild(k2).getInclusiveDiffScore();
				
				k1++;
				k2++;
			}
			// At different children
			else {
				if (trace1.cfgNode == null || trace2.cfgNode == null)
					return mergeProfileNode(trace1, w1, trace2, w2);
				// Increase k1 if
				if (k1 < trace1.getNumOfChildren() && 
				      (k2 == trace2.getNumOfChildren() || // k2 points to the end
					    (trace1.cfgNode.getChildIndex(trace1.getChildCFGNode(k1)) // k1 is prior of k2 in the CFG
						< trace2.cfgNode.getChildIndex(trace2.getChildCFGNode(k2))) || 
							((trace1.cfgNode.getChildIndex(trace1.getChildCFGNode(k1)) // k1 takes the same location as k2 in the CFG but its CCT ID is smaller
							 == trace2.cfgNode.getChildIndex(trace2.getChildCFGNode(k2)))
							 && (trace1.getID() < trace2.getID()))
					   )) {
					// merge node
					startExclusive2 += trace1.getChildTime(k1).getStartTimeExclusive() - startExclusive1;
					startInclusive2 += trace1.getChildTime(k1).getStartTimeInclusive() - startInclusive1;
					startExclusive2 = Math.min(startExclusive2, 
							(k2 == trace2.getNumOfChildren()) ? trace2.getTime().getEndTimeInclusive() : trace2.getChildTime(k2).getStartTimeExclusive());
					startInclusive2 = Math.min(startInclusive2, 
							(k2 == trace2.getNumOfChildren()) ? trace2.getTime().getEndTimeExclusive() : trace2.getChildTime(k2).getStartTimeInclusive());
					
					FunctionTrace dummeyTrace2 = new FunctionTrace(trace1.getChild(k1).getID(), trace1.getChild(k1).getName(), 
							trace1.getChild(k1).getDepth(), 
							(trace1.getChild(k1) instanceof AbstractTraceNode) ? ((AbstractTraceNode)trace1.getChild(k1)).cfgNode : null, 
							trace1.getChildCFGNode(k1));
					dummeyTrace2.setWeight(trace2.getWeight());
					dummeyTrace2.getTime().setStartTimeExclusive(startExclusive2);
					dummeyTrace2.getTime().setStartTimeInclusive(startInclusive2);
					dummeyTrace2.getTime().setEndTimeInclusive(startExclusive2);
					dummeyTrace2.getTime().setEndTimeExclusive(startInclusive2);
					
					AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), w1, dummeyTrace2, w2);
					if (mergedChild instanceof AbstractTraceNode) 
						mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime(), trace1.getChildCFGNode(k1));
					else 
						mergedTrace.addChild(mergedChild, mergeTimeStruct(trace1.getChildTime(k1), w1, dummeyTrace2.getTime(), w2), trace1.getChildCFGNode(k1));
					
					startExclusive1 = trace1.getChildTime(k1).getEndTimeInclusive();
					startInclusive1 = trace1.getChildTime(k1).getEndTimeExclusive();
					
					// compute diff score
					gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
					gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
					
					inclusiveDiff += trace1.getChild(k1).getDuration() * w1 * w2;
					
					k1++;
				}
				else {
					// merge node
					startExclusive1 += trace2.getChildTime(k2).getStartTimeExclusive() - startExclusive2;
					startInclusive1 += trace2.getChildTime(k2).getStartTimeInclusive() - startInclusive2;
					startExclusive1 = Math.min(startExclusive1, 
							(k1 == trace1.getNumOfChildren()) ? trace1.getTime().getEndTimeInclusive() : trace1.getChildTime(k1).getStartTimeExclusive());
					startInclusive1 = Math.min(startInclusive1, 
							(k1 == trace1.getNumOfChildren()) ? trace1.getTime().getEndTimeExclusive() : trace1.getChildTime(k1).getStartTimeInclusive());
										
					FunctionTrace dummeyTrace1 = new FunctionTrace(trace2.getChild(k2).getID(), trace2.getChild(k2).getName(), 
							trace2.getChild(k2).getDepth(), 
							(trace2.getChild(k2) instanceof AbstractTraceNode) ? ((AbstractTraceNode)trace2.getChild(k2)).cfgNode : null, 
							trace2.getChildCFGNode(k2));
					dummeyTrace1.setWeight(trace1.getWeight());
					dummeyTrace1.getTime().setStartTimeExclusive(startExclusive1);
					dummeyTrace1.getTime().setStartTimeInclusive(startInclusive1);
					dummeyTrace1.getTime().setEndTimeInclusive(startExclusive1);
					dummeyTrace1.getTime().setEndTimeExclusive(startInclusive1);
					
					AbstractTreeNode mergedChild = mergeNode(dummeyTrace1, w1, trace2.getChild(k2), w2);
					if (mergedChild instanceof AbstractTraceNode) 
						mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime(), trace2.getChildCFGNode(k2));
					else 
						mergedTrace.addChild(mergedChild, mergeTimeStruct(dummeyTrace1.getTime(), w1, trace2.getChildTime(k2), w2), trace2.getChildCFGNode(k2));
					
					startExclusive2 = trace2.getChildTime(k2).getEndTimeInclusive();
					startInclusive2 = trace2.getChildTime(k2).getEndTimeExclusive();
					
					// compute diff score
					gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
					gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
					
					inclusiveDiff += trace2.getChild(k2).getDuration() * w1 * w2;
					
					k2++;
				}
			}
		}

		// final gap
		gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
		gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
		gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
		gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
		inclusiveDiff += computeRangeDiff(gapDiffMin, gapDiffMax, 0, 0) * w1 * w2;
		
		long exclusiveDiff = computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
				trace2.getMinDuration(), trace2.getMaxDuration()) * w1 * w2 + trace1.getExclusiveDiffScore() + trace2.getExclusiveDiffScore();
		
		inclusiveDiff = Math.max(inclusiveDiff, computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
				trace2.getMinDuration(), trace2.getMaxDuration()) * w1 * w2 + trace1.getInclusiveDiffScore() + trace2.getInclusiveDiffScore());
		
		mergedTrace.setExclusiveDiffScore(exclusiveDiff);
		mergedTrace.setInclusiveDiffScore(inclusiveDiff);
		
		return mergedTrace;
	}
	
	static private ProfileNode mergeProfileNode(AbstractTreeNode node1, int weight1, AbstractTreeNode node2, int weight2) {
		ProfileNode prof1 = ProfileNode.toProfile(node1);
		ProfileNode prof2 = ProfileNode.toProfile(node2);
		
		//TODO substitute node1/node2 with prof1/prof2 to see if results change.
		long diff = computeProfileDiff(prof1, prof2) * weight1 * weight2 + node1.getInclusiveDiffScore() + node2.getInclusiveDiffScore();
		
		prof1.stretch(weight1, 1);
		prof2.stretch(weight2, 1);
		
		prof1.merge(prof2);
		
		prof1.stretch(1, weight1 + weight2);
		
		prof1.setWeight(node1.getWeight() + node2.getWeight());
		
		prof1.setExclusiveDiffScore(diff);
		prof1.setInclusiveDiffScore(diff);
		return prof1;
	}
	
	static private AbstractTreeNode mergeClusterNode(ClusterTreeNode node1, int weight1, ClusterTreeNode node2, int weight2) {
		long diff = computeDiff(node1, node2, false);
		Cluster[] cluster = new Cluster[node1.getNumOfClusters() + node2.getNumOfClusters()];
		
		for (int i = 0; i < node1.getNumOfClusters(); i++)
			cluster[i] = node1.getCluster(i);
		for (int i = 0; i < node2.getNumOfClusters(); i++)
			cluster[i+node1.getNumOfClusters()] = node2.getCluster(i);
		
		int numMembers = 0;
		for (Cluster c : cluster) 
			numMembers += c.getWeight();
		
		cluster = mergeCluster(cluster, maxNumOfClusters(numMembers));
		
		AbstractTreeNode mergedNode = null;
		
		if (cluster != null) {
			long minDuration = computeWeightedAverage(node1.getMinDuration(), weight1, node2.getMinDuration(), weight2);
			long maxDuration = computeWeightedAverage(node1.getMaxDuration(), weight1, node2.getMaxDuration(), weight2);
			
			mergedNode = new ClusterTreeNode(node1, node2, cluster, minDuration, maxDuration);
		}
		else
			mergedNode = mergeProfileNode(node1, weight1, node2, weight2);
		
		mergedNode.setExclusiveDiffScore(diff);
		mergedNode.setInclusiveDiffScore(diff);
		return mergedNode;
	}
	
	static private AbstractTreeNode mergeNode(AbstractTreeNode node1, int weight1, AbstractTreeNode node2, int weight2) {
		/*if (weight1 > weight2) return node1;
		if (weight2 > weight1) return node2;
		
		if (random.nextBoolean()) return node1;
		else return node2;*/
		
		if (node1.getWeight() != weight1) {
			System.err.println(node1.getWeight() + " vs " + weight1 + " : " + node1.toString(0, 0));
		}
		
		if (node2.getWeight() != weight2) {
			System.err.println(node2.getWeight() + " vs " + weight2 + " : " + node2.toString(0, 0));
		}
		
		
		if ((node1 instanceof FunctionTrace) && (node2 instanceof FunctionTrace))
			return mergeTraceNode((AbstractTraceNode)node1, weight1, (AbstractTraceNode)node2, weight2);
		else if ((node1 instanceof IterationTrace) && (node2 instanceof IterationTrace))
			return mergeTraceNode((AbstractTraceNode)node1, weight1, (AbstractTraceNode)node2, weight2);
		else if ((node1 instanceof ClusterTreeNode) && (node2 instanceof ClusterTreeNode))
			return mergeClusterNode((ClusterTreeNode)node1, weight1, (ClusterTreeNode)node2, weight2);
		//TODO IteratedLoopTrace
		//TODO Trace/Profile with Cluster
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
				diff[i][j] = (double)computeDiff(cluster[i].getRep(), cluster[j].getRep(), false)
					/ (double)(cluster[i].getRep().getDuration() + cluster[j].getRep().getDuration());
				maxDiff = Math.max(maxDiff, diff[i][j]);
				if (diff[i][j] < minDiff) {
					minDiff = diff[i][j];
					idx1 = i;
					idx2 = j;
				}
			}
/*
if (cluster[0].getRep().getID() == 7159) {
	System.out.println("*****************************************************");
	for (int i = 0; i < numCluster; i++) {
		System.out.println(cluster[i].getMembers());
		System.out.print(cluster[i].getRep().toString(0, 0));
		for (int j = i+1; j < numCluster; j++)
			System.out.print("\t" + diff[i][j]);
		System.out.println();
	}
	System.out.println("minDiff = " + minDiff + " @ (" + idx1 + ", " + idx2 + ")");
}*/

		/**
		 * Merge the closest two clusters if
		 * 1. minDiff is too small;
		 * 2. minDiff is too small compared to maxDiff (and not greater than maxClusterDiff);
		 * 3. minDiff is less than maxClusterDiff and numCluster exceeds the threshold.
		 */
		while (minDiff <= minClusterDiff 
				|| minDiff <= Math.min(maxDiff/maxMinRatio, maxClusterDiff)
				|| (minDiff <= maxClusterDiff && numCluster > maxNumCluster)) {
			AbstractTreeNode node = mergeNode(cluster[idx1].getRep(), cluster[idx1].getWeight(), 
					  cluster[idx2].getRep(), cluster[idx2].getWeight());
			if (node == null) {
				System.out.println(cluster[idx1].getRep().toString(cluster[idx1].getRep().getDepth()+1, 0));
				System.out.println(cluster[idx2].getRep().toString(cluster[idx2].getRep().getDepth()+1, 0));
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
				diff[i][idx1] = (double)computeDiff(cluster[i].getRep(), cluster[idx1].getRep(), false)
						/ (double)(cluster[i].getRep().getDuration() + cluster[idx1].getRep().getDuration());
			for (int i = idx1+1; i < numCluster; i++)
				diff[idx1][i] = (double)computeDiff(cluster[idx1].getRep(), cluster[i].getRep(), false)
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
if (cluster[0].getRep().getID() == 7159) {
	System.out.println("*****************************************************");
	for (int i = 0; i < numCluster; i++) {
		System.out.println(cluster[i].getMembers());
		System.out.print(cluster[i].getRep().toString(0, 0));
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
		if (node instanceof ClusterTreeNode)
			((ClusterTreeNode) node).addLabel(ID);
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
	
	static public ClusterTreeNode findCluster(AbstractTraceNode loop) {
		testDiff(loop);
		Cluster[] cluster = findCluster(loop, 0, loop.getNumOfChildren()-1, maxNumOfClusters(loop.getNumOfChildren()));
		if (cluster == null) return null;
		else return new ClusterTreeNode(loop, cluster);
	}
	
	static public void testDiff(AbstractTreeNode node) {
		//if (node.getID() == 0) {
		if (node instanceof IteratedLoopTrace) {
			AbstractTraceNode loop = (AbstractTraceNode)node;
			if (loop.getID() == 7159 || loop.getID() == 1598) {
				System.out.println(loop.getName()+"("+loop.getID()+")");
				System.out.print(loop.toString(loop.getDepth(), 0));
				for (int i = 0; i < loop.getNumOfChildren(); i++) {
					for (int j = 0; j < loop.getNumOfChildren(); j++) {
						long diff = mergeNode(loop.getChild(i), 1, loop.getChild(j), 1).getInclusiveDiffScore();
						boolean test = (diff != computeDiff(loop.getChild(i), loop.getChild(j), false));
						diff = diff * 10000 / (loop.getChild(i).getDuration() + loop.getChild(j).getDuration());
						System.out.print("\t" + diff/100 + "." + diff%100/10 + diff%10);
						if (test) System.out.print("*");
					}
					System.out.println();
				}
				System.out.println();
			}
			//testDiff(loop.getChild(loop.getNumOfChildren()-1));
		}
		/*else if (node instanceof AbstractTraceNode)
			for (int i = 0; i < ((AbstractTraceNode)node).getNumOfChildren(); i++) 
			testDiff(((AbstractTraceNode)node).getChild(i));*/
	}
}
