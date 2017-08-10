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
		return 27; //31 - Integer.numberOfLeadingZeros(numOfInstances) + 1; // which is log2(numOfInstance) + 1
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
				int compare = 0; // compare < 0 means k1 should be increased; compare > 0 means k2 should be increased.
				if (k1 == trace1.getNumOfChildren()) compare = 1; 
				if (k2 == trace2.getNumOfChildren()) compare = -1; // increase k1 if k2 points to end
				
				if (compare == 0) {
					assert (trace1.cfgNode == trace2.cfgNode);
					if (trace1.cfgNode == null || !trace1.cfgNode.valid)
						return computeProfileDiff(trace1, trace2);
					
					if (!trace1.cfgNode.hasChild(trace1.getChildCFGNode(k1))) compare = -1; // increase k1 if the current child's cfgNode is not found.
					if (!trace1.cfgNode.hasChild(trace2.getChildCFGNode(k2))) compare = 1;
				}
				
				if (compare == 0) // increase k1 if k1 is a predecessor of k2; increase k2 of k2 is a predecessor of k1.
					compare = trace1.cfgNode.compareChild(trace1.getChildCFGNode(k1), trace2.getChildCFGNode(k2));

				if (compare == 0) // tie is broken by simply comparing CCT ID
					compare = trace1.getChild(k1).getID() - trace2.getChild(k2).getID();
				
				if (compare < 0) { // increase k1
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

		if (clusterDebug && node1.getID() == clusterDebugID) {
			System.err.println("Compute: ");
			for (int i = 0; i < numCluster1; i++) {
				for (int j = 0; j < numCluster2; j++) {
					System.err.print(diffScore[i][j] + "  ");
				}
				System.err.println();
			}
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
			System.out.println(str + node1.getID() + " vs. " + node2.getID() + " " + node1.getName() + " = " + diff);
		}
		return diff;
	}
	
	static private boolean addTraceDiffScore(AbstractTraceNode dest, AbstractTraceNode src) {
		int k1 = 0, k2 = 0;
		boolean ret = true;
		while (k1 < dest.getNumOfChildren() && k2 < src.getNumOfChildren())
			if (dest.getChild(k1).getID() == src.getChild(k2).getID()) {
				ret &= addDiffScore(dest.getChild(k1), src.getChild(k2));
				k1++;
				k2++;
			}
			else 
				k1++;
		
		dest.setExclusiveDiffScore(dest.getExclusiveDiffScore() + src.getExclusiveDiffScore());
		dest.setInclusiveDiffScore(dest.getInclusiveDiffScore() + src.getInclusiveDiffScore());
		
		return ret;
	}
	
	static private boolean addProfileDiffScore(ProfileNode dest, AbstractTreeNode src) {
		dest.setExclusiveDiffScore(dest.getExclusiveDiffScore() + src.getExclusiveDiffScore());
		dest.setInclusiveDiffScore(dest.getInclusiveDiffScore() + src.getInclusiveDiffScore());
		return true;
	}
	
	static private boolean addClusterDiffScore(ClusterTreeNode dest, ClusterTreeNode src) {
		dest.setExclusiveDiffScore(dest.getExclusiveDiffScore() + src.getExclusiveDiffScore());
		dest.setInclusiveDiffScore(dest.getInclusiveDiffScore() + src.getInclusiveDiffScore());
		addDiffScore(dest.getRep(), src.getRep());
		return true;
		//TODO need some deeper thought
	}
	
	/**
	 * Add the diff scores in src and its sub-nodes to corresponding nodes in dest.
	 */
	static private boolean addDiffScore(AbstractTreeNode dest, AbstractTreeNode src) {
		if ((dest instanceof AbstractTraceNode) && (src instanceof AbstractTraceNode))
			return addTraceDiffScore((AbstractTraceNode)dest, (AbstractTraceNode)src);
		else if ((dest instanceof ClusterTreeNode) && (src instanceof ClusterTreeNode))
			return addClusterDiffScore((ClusterTreeNode)dest, (ClusterTreeNode)src);
		else if (dest instanceof ProfileNode)
			return addProfileDiffScore((ProfileNode)dest, src);
		else {
			System.err.println("Error while adding diffscore at " + dest.getID());
			System.err.println(dest.toString(dest.getDepth(), 0, 0));
			System.err.println(src.toString(src.getDepth(), 0, 0));
			return false;
		}
	}
	
	/*
	 * Node merge functions.
	 */
	static public long computeWeightedAverage(long value1, int weight1, long value2, int weight2) {
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
	
	static private AbstractTreeNode mergeTraceNode(AbstractTraceNode trace1, int weight1, AbstractTraceNode trace2, int weight2, boolean accumulate) {
		AbstractTraceNode mergedTrace = (AbstractTraceNode)trace1.duplicate();
		
		int w1 = trace1.getWeight();
		int w2 = trace2.getWeight();
		mergedTrace.setTime(mergeTimeStruct(trace1.getTime(), w1, trace2.getTime(), w2));
		mergedTrace.setWeight(w1 + w2);
		mergedTrace.clearChildren();
	
		int k1 = 0;
		int k2 = 0;
		
		/**
		 * When accumulating difference scores, the inclusive difference score of the merged trace consists of the following components --
		 *  1) the inclusive difference score of all children, which can be divided to:
		 *    1.1) the inclusive difference score of all children in trace1;
		 *    1.2) the inclusive difference score of all children in trace2;
		 *    1.3) the difference score of children between trace1 and trace2;
		 *  2) the difference score of gaps among all children, which can be divided to:
		 *    2.1) the difference score of gaps among children in trace1;
		 *    2.2) the difference score of gaps among children in trace2;
		 *    2.3) the newly introduced difference score of gaps among children in merged trace.
		 *  
		 *  When not accumulating, only 1.3) and 2.3) needs to be added up.
		 */
		long inclusiveDiff = 0;
		
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
				AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), w1, trace2.getChild(k2), w2, accumulate);
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
				
				inclusiveDiff += diff * w1 * w2; // belongs to 2.3)
				inclusiveDiff += mergedChild.getInclusiveDiffScore(); // belongs to 1.3) when not accumulating and 1) when accumulating
				
				k1++;
				k2++;
			}
			// At different children
			else {
				int compare = 0; // compare < 0 means k1 should be increased; compare > 0 means k2 should be increased.
				if (k1 == trace1.getNumOfChildren()) compare = 1; 
				if (k2 == trace2.getNumOfChildren()) compare = -1; // increase k1 if k2 points to end
				
				if (compare == 0) {
					assert (trace1.cfgNode == trace2.cfgNode);
					if (trace1.cfgNode == null || !trace1.cfgNode.valid)
						return mergeProfileNode(trace1, w1, trace2, w2, accumulate);
					
					if (!trace1.cfgNode.hasChild(trace1.getChildCFGNode(k1))) compare = -1; // increase k1 if the current child's cfgNode is not found.
					if (!trace1.cfgNode.hasChild(trace2.getChildCFGNode(k2))) compare = 1;
				}
				
				if (compare == 0) // increase k1 if k1 is a predecessor of k2; increase k2 of k2 is a predecessor of k1.
					compare = trace1.cfgNode.compareChild(trace1.getChildCFGNode(k1), trace2.getChildCFGNode(k2));

				if (compare == 0) // tie is broken by simply comparing CCT ID
					compare = trace1.getChild(k1).getID() - trace2.getChild(k2).getID();
				
				if (compare < 0) { // increase k1
					startExclusive2 += trace1.getChildTime(k1).getStartTimeExclusive() - startExclusive1;
					startInclusive2 += trace1.getChildTime(k1).getStartTimeInclusive() - startInclusive1;
					startExclusive2 = Math.min(startExclusive2, 
							(k2 == trace2.getNumOfChildren()) ? trace2.getTime().getEndTimeInclusive() : trace2.getChildTime(k2).getStartTimeExclusive());
					startInclusive2 = Math.min(startInclusive2, 
							(k2 == trace2.getNumOfChildren()) ? trace2.getTime().getEndTimeExclusive() : trace2.getChildTime(k2).getStartTimeInclusive());
					
					AbstractTreeNode dummyNode = null;
					dummyNode = trace1.getChild(k1).voidDuplicate();

					TraceTimeStruct ts = new TraceTimeStruct();
					ts.setStartTimeExclusive(startExclusive2);
					ts.setStartTimeInclusive(startInclusive2);
					ts.setEndTimeInclusive(startExclusive2);
					ts.setEndTimeExclusive(startInclusive2);
					
					if (dummyNode instanceof AbstractTraceNode) 
						((AbstractTraceNode) dummyNode).setTime(ts);
					
					dummyNode.setWeight(trace2.getWeight());

					AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), w1, dummyNode, w2, accumulate);
					if (mergedChild instanceof AbstractTraceNode) 
						mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime(), trace1.getChildCFGNode(k1));
					else 
						mergedTrace.addChild(mergedChild, mergeTimeStruct(trace1.getChildTime(k1), w1, ts, w2), trace1.getChildCFGNode(k1));
					
					startExclusive1 = trace1.getChildTime(k1).getEndTimeInclusive();
					startInclusive1 = trace1.getChildTime(k1).getEndTimeExclusive();
					
					// compute diff score
					gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
					gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
					
					inclusiveDiff += mergedChild.getInclusiveDiffScore(); // belongs to 1.3) when not accumulating and 1) when accumulating

					k1++;
				}
				else {
					startExclusive1 += trace2.getChildTime(k2).getStartTimeExclusive() - startExclusive2;
					startInclusive1 += trace2.getChildTime(k2).getStartTimeInclusive() - startInclusive2;
					startExclusive1 = Math.min(startExclusive1, 
							(k1 == trace1.getNumOfChildren()) ? trace1.getTime().getEndTimeInclusive() : trace1.getChildTime(k1).getStartTimeExclusive());
					startInclusive1 = Math.min(startInclusive1, 
							(k1 == trace1.getNumOfChildren()) ? trace1.getTime().getEndTimeExclusive() : trace1.getChildTime(k1).getStartTimeInclusive());
										
					AbstractTreeNode dummyNode = null;
					dummyNode = trace2.getChild(k2).voidDuplicate();

					TraceTimeStruct ts = new TraceTimeStruct();
					ts.setStartTimeExclusive(startExclusive1);
					ts.setStartTimeInclusive(startInclusive1);
					ts.setEndTimeInclusive(startExclusive1);
					ts.setEndTimeExclusive(startInclusive1);
					
					if (dummyNode instanceof AbstractTraceNode) 
						((AbstractTraceNode) dummyNode).setTime(ts);
					
					dummyNode.setWeight(trace1.getWeight());
					
					AbstractTreeNode mergedChild = mergeNode(dummyNode, w1, trace2.getChild(k2), w2, accumulate);
					if (mergedChild instanceof AbstractTraceNode) 
						mergedTrace.addChild(mergedChild, ((AbstractTraceNode)mergedChild).getTime(), trace2.getChildCFGNode(k2));
					else 
						mergedTrace.addChild(mergedChild, mergeTimeStruct(ts, w1, trace2.getChildTime(k2), w2), trace2.getChildCFGNode(k2));
					
					startExclusive2 = trace2.getChildTime(k2).getEndTimeInclusive();
					startInclusive2 = trace2.getChildTime(k2).getEndTimeExclusive();
					
					// compute diff score
					gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
					gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
					
					inclusiveDiff += mergedChild.getInclusiveDiffScore(); // belongs to 1.3) when not accumulating and 1) when accumulating
					k2++;
				}
			}
		}

		// final gap
		gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
		gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
		gapDiffMin -= trace2.getMaxGapDurationBeforeChild(k2);
		gapDiffMax -= trace2.getMinGapDurationBeforeChild(k2);
		inclusiveDiff += computeRangeDiff(gapDiffMin, gapDiffMax, 0, 0) * w1 * w2; // belongs to 2.3)
		
		long exclusiveDiff = computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
				trace2.getMinDuration(), trace2.getMaxDuration()) * w1 * w2;
		
		if (accumulate) {
			exclusiveDiff += trace1.getExclusiveDiffScore() + trace2.getExclusiveDiffScore();
			
			// The above code has added 1) and 2.3) to inclusive diff score. Add 2.1) and 2.2) to inclusive diff score below.
			
			// Add 2.1)
			inclusiveDiff += trace1.getInclusiveDiffScore();
			for (int i = 0; i < trace1.getNumOfChildren(); i++)
				inclusiveDiff -= trace1.getChild(i).getInclusiveDiffScore();
			
			// Add 2.2)
			inclusiveDiff += trace2.getInclusiveDiffScore();
			for (int i = 0; i < trace2.getNumOfChildren(); i++)
				inclusiveDiff -= trace2.getChild(i).getInclusiveDiffScore();
		}
		
		if (accumulate) 
			inclusiveDiff = Math.max(inclusiveDiff, computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
				trace2.getMinDuration(), trace2.getMaxDuration()) * w1 * w2 + trace1.getInclusiveDiffScore() + trace2.getInclusiveDiffScore());
		else
			inclusiveDiff = Math.max(inclusiveDiff, computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
					trace2.getMinDuration(), trace2.getMaxDuration()) * w1 * w2);
		
		/*
		if (accumulate)
			if (inclusiveDiff != trace1.getInclusiveDiffScore() + trace2.getInclusiveDiffScore() + mergeTraceNode(trace1, w1, trace2, w2, false).getInclusiveDiffScore()) {
					System.err.println("Merged: " + mergedTrace.toString(mergedTrace.getDepth()+2,0));
					System.err.println("Trace1@" + w1 + ": "+ trace1.toString(mergedTrace.getDepth()+2,0));
					System.err.println("Trace2@" + w2 + ": "+ trace2.toString(mergedTrace.getDepth()+2,0));
					System.err.println(inclusiveDiff + " " + trace1.getInclusiveDiffScore() + " " + trace2.getInclusiveDiffScore() + 
							" " + mergeTraceNode(trace1, w1, trace2, w2, false).getInclusiveDiffScore() + " " + computeTraceDiff(trace1, trace2, false));
			}
		*/
		
		mergedTrace.setExclusiveDiffScore(exclusiveDiff);
		mergedTrace.setInclusiveDiffScore(inclusiveDiff);
		
		return mergedTrace;
	}
	
	
	static private ProfileNode mergeProfileNode(AbstractTreeNode node1, int weight1, AbstractTreeNode node2, int weight2, boolean accumulate) {
		ProfileNode prof1 = ProfileNode.toProfile(node1);
		ProfileNode prof2 = ProfileNode.toProfile(node2);
		
		//TODO substitute node1/node2 with prof1/prof2 to see if results change.
		long diff = computeProfileDiff(prof1, prof2) * weight1 * weight2;
		if (accumulate) diff += node1.getInclusiveDiffScore() + node2.getInclusiveDiffScore();
		
		prof1.stretch(weight1, 1);
		prof2.stretch(weight2, 1);
		
		prof1.merge(prof2);
		
		prof1.stretch(1, weight1 + weight2);
		
		prof1.setWeight(node1.getWeight() + node2.getWeight());
		
		prof1.setExclusiveDiffScore(diff);
		prof1.setInclusiveDiffScore(diff);
		return prof1;
	}
	
	static private AbstractTreeNode mergeClusterNode(ClusterTreeNode node1, int weight1, ClusterTreeNode node2, int weight2, boolean accumulate) {
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
		if (cluster == null) mergedNode = mergeProfileNode(node1, weight1, node2, weight2, accumulate);
		else {
			AbstractTreeNode rep = computeAveragedRep(cluster);
			
			rep.clearDiffScore();
			//rep.setWeight(node1.getRep().getWeight() + node2.getRep().getWeight());
			
			/**
			 * Next, we are going to compute the diff score between node1 and node2.
			 * 
			 * Suppose that the merged ClusterNode has a weight of W (= w1 + w2) and an average of K iterations, 
			 * 		where K = Sum(weight of rep nodes) / W.
			 * 
			 * To calculate the difference score among node1 and node2, we first need to normalize their weight to w1*w2
			 * by multiplying the weight of each cluster in node1 with w2 and the weight of each cluster in node2 with w1. 
			 * 
			 * We then try to match clusters in node1 with the ones in node2 in a way that minimizes the difference score.
			 * We use greedy algorithm for match (may not be optimal) and add up the difference scores.
			 * 
			 * We are essentially adding up difference scores of K*w1*w2 iterations, which is exactly diff(node1,node2).
			 */
			
			int numCluster1 = node1.getNumOfClusters();
			int numCluster2 = node2.getNumOfClusters();
			
			AbstractTreeNode[] rep1 = new AbstractTreeNode[numCluster1];
			int[] numMember1 = new int[numCluster1];
			for (int i = 0; i < numCluster1; i++) {
				rep1[i] = node1.getCluster(i).getRep();
				numMember1[i] = rep1[i].getWeight() * node2.getWeight();
			}
			
			AbstractTreeNode[] rep2 = new AbstractTreeNode[numCluster2];
			int[] numMember2 = new int[numCluster2];
			for (int i = 0; i < numCluster2; i++) {
				rep2[i] = node2.getCluster(i).getRep();
				numMember2[i] = rep2[i].getWeight() * node1.getWeight();
			}
			
			if (clusterDebug && node1.getID() == clusterDebugID) {
				System.err.println("Merge start: ");
			}
				
			AbstractTreeNode[][] diffTreeNode = new AbstractTreeNode[numCluster1][numCluster2];
			double[][] diffRatio = new double[numCluster1][numCluster2];
			for (int i = 0; i < numCluster1; i++)
				for (int j = 0; j < numCluster2; j++) {
					diffTreeNode[i][j] = mergeNode(rep1[i], rep1[i].getWeight(), rep2[j], rep2[j].getWeight(), false);
					diffRatio[i][j] = ((double)diffTreeNode[i][j].getInclusiveDiffScore()) / (rep1[i].getWeight() * rep2[j].getWeight()) / (double)(rep1[i].getDuration() + rep2[j].getDuration());
				}

			if (clusterDebug && node1.getID() == clusterDebugID) {
				System.err.println("Merge: ");
				for (int i = 0; i < numCluster1; i++) {
					for (int j = 0; j < numCluster2; j++) {
						System.err.print(diffTreeNode[i][j].getInclusiveDiffScore() / (rep1[i].getWeight() * rep2[j].getWeight()) + "  ");
					}
					System.err.println();
				}
				
				System.err.println(rep1[0].getWeight() + " " + rep2[0].getWeight());
				
				System.err.println(diffTreeNode[0][0].toString(diffTreeNode[0][0].getDepth()+3, 0, 0));
				
				computeDiff(rep1[0], rep2[0], true);
				clusterDebug = false;
			}
			
			while (numCluster1 > 0 && numCluster2 > 0) {
				int idx1 = 0, idx2 = 0;
				for (int i = 0; i < numCluster1; i++)
					for (int j = 0; j < numCluster2; j++) 
						if (diffRatio[i][j] < diffRatio[idx1][idx2]) {
							idx1 = i;
							idx2 = j;
						}
				
				int numMatched = Math.min(numMember1[idx1], numMember2[idx2]);
				
				diffTreeNode[idx1][idx2].stretchDiffScore(numMatched, rep1[idx1].getWeight() * rep2[idx2].getWeight());
				addDiffScore(rep, diffTreeNode[idx1][idx2]);

				if (clusterDebug && node1.getID() == clusterDebugID) {
					System.err.println("diff = " + rep.getInclusiveDiffScore());
				}
				
				numMember1[idx1] -= numMatched;
				numMember2[idx2] -= numMatched;
				
				// Move the last cluster to the current position
				if (numMember1[idx1] == 0) {
					numCluster1 --;
					rep1[idx1] = rep1[numCluster1];
					numMember1[idx1] = numMember1[numCluster1];
					for (int j = 0; j < numCluster2; j++) {
						diffTreeNode[idx1][j] = diffTreeNode[numCluster1][j];
						diffRatio[idx1][j] = diffRatio[numCluster1][j];
					}
				}
				
				if (numMember2[idx2] == 0) {
					numCluster2 --;
					rep2[idx2] = rep2[numCluster2];
					numMember2[idx2] = numMember2[numCluster2];
					for (int i = 0; i < numCluster1; i++) {
						diffTreeNode[i][idx2] = diffTreeNode[i][numCluster2];
						diffRatio[i][idx2] = diffRatio[i][numCluster2];
					}
				}
			}
			
			while (numCluster1 > 0) {
				numCluster1 --;
				AbstractTreeNode dummyNode = rep1[numCluster1].voidDuplicate();
				dummyNode.setWeight(1);
				AbstractTreeNode diff = mergeNode(rep1[numCluster1], rep1[numCluster1].getWeight(), dummyNode, 1, false);
				diff.stretchDiffScore(numMember1[numCluster1], rep1[numCluster1].getWeight());
				addDiffScore(rep, diff);
			}
			
			if (clusterDebug && node1.getID() == clusterDebugID) {
				System.err.println("diff = " + rep.getInclusiveDiffScore());
			}

			while (numCluster2 > 0) {
				numCluster2 --;
				AbstractTreeNode dummyNode = rep2[numCluster2].voidDuplicate();
				dummyNode.setWeight(1);
				AbstractTreeNode diff = mergeNode(dummyNode, 1, rep2[numCluster2], rep2[numCluster2].getWeight(), false);
				diff.stretchDiffScore(numMember2[numCluster2], rep2[numCluster2].getWeight());
				addDiffScore(rep, diff);
			}

			if (clusterDebug && node1.getID() == clusterDebugID) {
				System.err.println("diff = " + rep.getInclusiveDiffScore());
			}
			
			long diff = computeClusterDiff(node1, node2, false);
			if (rep.getInclusiveDiffScore() / (node1.getWeight() * node2.getWeight()) != diff) {
				System.err.println("Different result for Cluster " + node1.getID() + ": " + diff + " vs " + rep.getInclusiveDiffScore() / (node1.getWeight() * node2.getWeight()));
				//System.err.println(node1.getCluster(0).toString(node1.getDepth()+3, 0));
				//System.err.println(node2.getCluster(0).toString(node2.getDepth()+3, 0));
			}
			
			if (accumulate) {
				addDiffScore(rep, node1.getRep());
				addDiffScore(rep, node2.getRep());
			}
			
			mergedNode = new ClusterTreeNode(node1, node2, rep, cluster);
		}

		return mergedNode;
	}
	
	static private boolean clusterDebug = false;
	static private int clusterDebugID = 7849;
	
	static private AbstractTreeNode mergeNode(AbstractTreeNode node1, int weight1, AbstractTreeNode node2, int weight2, boolean accumulate) {
		if (node1.getWeight() != weight1) {
			System.err.println(node1.getWeight() + " vs " + weight1 + " : " + node1.toString(0, 0, 0));
		}
		
		if (node2.getWeight() != weight2) {
			System.err.println(node2.getWeight() + " vs " + weight2 + " : " + node2.toString(0, 0, 0));
		}
		
		
		if ((node1 instanceof AbstractTraceNode) && (node2 instanceof AbstractTraceNode))
			return mergeTraceNode((AbstractTraceNode)node1, weight1, (AbstractTraceNode)node2, weight2, accumulate);
		else if ((node1 instanceof IterationTrace) && (node2 instanceof IterationTrace))
			return mergeTraceNode((AbstractTraceNode)node1, weight1, (AbstractTraceNode)node2, weight2, accumulate);
		else if ((node1 instanceof ClusterTreeNode) && (node2 instanceof ClusterTreeNode))
			return mergeClusterNode((ClusterTreeNode)node1, weight1, (ClusterTreeNode)node2, weight2, accumulate);
		//TODO IteratedLoopTrace
		//TODO Trace/Profile with Cluster
		else return mergeProfileNode(node1, weight1, node2, weight2, accumulate);
		
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
			AbstractTreeNode node = mergeNode(cluster[idx1].getRep(), cluster[idx1].getWeight(), 
					  cluster[idx2].getRep(), cluster[idx2].getWeight(), true);

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
	
	static private AbstractTreeNode computeAveragedRep(Cluster[] cluster) {
		AbstractTreeNode node = cluster[0].getRep();
		for (int i = 1; i < cluster.length; i++)
			node = mergeNode(node, node.getWeight(), cluster[i].getRep(), cluster[i].getWeight(), false);
		node.clearDiffScore();
		
		for (int i = 0; i < cluster.length; i++)
			addDiffScore(node, cluster[i].getRep());
		
		for (int i = 0; i < cluster.length; i++)
			for (int j = i+1; j < cluster.length; j++) {
				AbstractTreeNode diff = mergeNode(cluster[i].getRep(), cluster[i].getWeight(), cluster[j].getRep(), cluster[j].getWeight(), false);
				addDiffScore(node, diff);
			}
		return node;
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
		AbstractTraceNode dupLoop = (AbstractTraceNode) loop.duplicate();
		dupLoop.clearDiffScore();
		Cluster[] cluster = findCluster(dupLoop, 0, dupLoop.getNumOfChildren()-1, maxNumOfClusters(dupLoop.getNumOfChildren()));
		if (cluster == null) return null;
		else return new ClusterTreeNode(loop, computeAveragedRep(cluster), cluster);
	}
	
	static public void testDiff(AbstractTreeNode node) {
		//if (node.getID() == 0) {
		if (node instanceof IteratedLoopTrace) {
			AbstractTraceNode loop = (AbstractTraceNode)node;
			if (loop.getID() == 7159 || loop.getID() == 1598 || loop.getID() == 36268) {
				System.out.println(loop.getName()+"("+loop.getID()+")");
				System.out.print(loop.toString(loop.getDepth(), 0, 0));
				for (int i = 0; i < loop.getNumOfChildren(); i++) {
					for (int j = 0; j < loop.getNumOfChildren(); j++) {
						AbstractTreeNode diffNode = mergeNode(loop.getChild(i), 1, loop.getChild(j), 1, false);
						long diff = diffNode.getInclusiveDiffScore();
						//boolean test = (diff != computeDiff(loop.getChild(i), loop.getChild(j), false));
						diff = diff * 10000 / (loop.getChild(i).getDuration() + loop.getChild(j).getDuration());
						System.out.print("\t" + diff/100 + "." + diff%100/10 + diff%10);
						//if (test) System.out.print("*");
						//System.out.print("\t" + diff);
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
