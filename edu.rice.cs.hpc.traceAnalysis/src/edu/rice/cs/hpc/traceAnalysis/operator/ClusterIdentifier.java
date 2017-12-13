package edu.rice.cs.hpc.traceAnalysis.operator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.Cluster;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterSetNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IteratedLoopTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IterationTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ProfileNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ShadowTraceTree;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTimeStruct;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTree;
import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

public class ClusterIdentifier {
	static public final double minClusterDiff = 0.01;
	static public final double maxClusterDiff = 0.20;
	static public final double maxMinRatio = 4;
	
	public final String rankName;
	public final TraceTree tree;
	private long clusterCount;
	
	public ClusterIdentifier(String rankName, TraceTree tree) {
		this.rankName = rankName;
		this.tree = tree;
		this.clusterCount = 0;
	}
	
	public int maxNumOfClusters(int numOfInstances) {
		return (int)Math.round(Math.sqrt(numOfInstances)) + 20; //31 - Integer.numberOfLeadingZeros(numOfInstances) + 1; // which is log2(numOfInstance) + 1
		//return 10;
	}

	private long computeRangeDiff(long min1, long max1, long min2, long max2) {
		if (max1 - min1 > 50000) {
			long mid1 = (max1 + min1) / 2;
			min1 = mid1 - 25000;
			max1 = mid1 + 25000;
		}
		
		if (max2 - min2 > 50000) {
			long mid2 = (max2 + min2) / 2;
			min2 = mid2 - 25000;
			max2 = mid2 + 25000;
		}
		
		if (max2 < min1) return min1 - max2;
		if (max1 < min2) return min2 - max1;
		return 0;
	}

	private void addTraceDiffScore(AbstractTraceNode dest, AbstractTraceNode src) {
		int k1 = 0, k2 = 0;
		while (k1 < dest.getNumOfChildren() && k2 < src.getNumOfChildren())
			if (dest.getChild(k1).getID() == src.getChild(k2).getID()) {
				addDiffScore(dest.getChild(k1), src.getChild(k2));
				k1++;
				k2++;
			}
			else 
				k1++;
		
		dest.setExclusiveDiffScore(dest.getExclusiveDiffScore() + src.getExclusiveDiffScore());
		dest.setInclusiveDiffScore(dest.getInclusiveDiffScore() + src.getInclusiveDiffScore());
		
		if (k2 != src.getNumOfChildren()) { 
			System.err.println("Error while adding trace diff.");
		}
	}
	
	private void addProfileDiffScore(ProfileNode dest, AbstractTreeNode src) {
		ProfileNode srcProf;
		if (src instanceof ProfileNode) srcProf = (ProfileNode) src;
		else srcProf = ProfileNode.toProfile(src);
		
		dest.setExclusiveDiffScore(dest.getExclusiveDiffScore() + srcProf.getExclusiveDiffScore());
		dest.setInclusiveDiffScore(dest.getInclusiveDiffScore() + srcProf.getInclusiveDiffScore());
		
		for (ProfileNode child : srcProf.getChildMap().values())
			if (dest.getChildMap().containsKey(child.getID()))
				addDiffScore(dest.getChildMap().get(child.getID()), child);
			else {
				System.err.println("Error while adding profile diff -- child " + child.getID() + " not found.");
				//System.err.println(dest.toString(dest.getDepth()+1, 0, 2));
				//System.err.println(src.toString(src.getDepth()+1, 0, 2));
			}
	}
	
	private void addClusterDiffScore(ClusterSetNode dest, ClusterSetNode src) {
		dest.setExclusiveDiffScore(dest.getExclusiveDiffScore() + src.getExclusiveDiffScore());
		dest.setInclusiveDiffScore(dest.getInclusiveDiffScore() + src.getInclusiveDiffScore());
		dest.setMaxDurationRep(Math.max(dest.getMaxDurationRep(), src.getMaxDurationRep()));
		
		addDiffScore(dest.getRep(), src.getRep());
		//TODO need some deeper thought
	}
	
	/**
	 * Add the diff scores in src and its sub-nodes to corresponding nodes in dest.
	 */
	private void addDiffScore(AbstractTreeNode dest, AbstractTreeNode src) {
		if (src.getInclusiveDiffScore() == 0) return;
		
		if ((dest instanceof AbstractTraceNode) && (src instanceof AbstractTraceNode))
			addTraceDiffScore((AbstractTraceNode)dest, (AbstractTraceNode)src);
		else if ((dest instanceof ClusterSetNode) && (src instanceof ClusterSetNode))
			addClusterDiffScore((ClusterSetNode)dest, (ClusterSetNode)src);
		else if (dest instanceof ProfileNode)
			addProfileDiffScore((ProfileNode)dest, src);
		else {
			System.err.println("Error while adding diffscore at " + dest.getID());
			System.err.println(dest.toString(dest.getDepth(), 0, 0));
			System.err.println(src.toString(src.getDepth(), 0, 0));
		}
	}
	
	private AbstractTreeNode mergeTraceNode(AbstractTraceNode trace1, int weight1, AbstractTraceNode trace2, int weight2, boolean accumulate, boolean scoreOnly) {
		int w1 = trace1.getWeight();
		int w2 = trace2.getWeight();
		
		AbstractTraceNode mergedTrace = null;
		
		if (!scoreOnly) {
			mergedTrace = (AbstractTraceNode)trace1.duplicate(); //TODO memory optimization
			mergedTrace.clearChildren();
			
			mergedTrace.setTraceTime(TraceTimeStruct.mergeTimeStruct(trace1.getTraceTime(), weight1, trace2.getTraceTime(), weight2));
			
			mergedTrace.setWeight(w1 + w2);
			mergedTrace.setInclusiveDiffScore(trace1.getInclusiveDiffScore() + trace2.getInclusiveDiffScore());
			mergedTrace.setExclusiveDiffScore(trace1.getExclusiveDiffScore() + trace2.getExclusiveDiffScore());
			mergedTrace.setMaxDurationRep(Math.max(trace1.getMaxDurationRep(), trace2.getMaxDurationRep()));
			mergedTrace.setMinDurationRep(Math.min(trace1.getMinDurationRep(), trace2.getMinDurationRep()));
			mergedTrace.setTotalDurationRep(trace1.getTotalDurationRep() + trace2.getTotalDurationRep());
		}
		else {
			mergedTrace = (AbstractTraceNode)trace1.voidDuplicate();
		}
	
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
		double inclusiveDiff = 0;
		
		long gapDiffMin = 0;
		long gapDiffMax = 0;
		
		long startExclusive1 = trace1.getTraceTime().getStartTimeExclusive();
		long startInclusive1 = trace1.getTraceTime().getStartTimeInclusive();
		long startExclusive2 = trace2.getTraceTime().getStartTimeExclusive();
		long startInclusive2 = trace2.getTraceTime().getStartTimeInclusive();
		
		while (k1 < trace1.getNumOfChildren() || k2 < trace2.getNumOfChildren()) {
			// At the same child 
			if (k1 < trace1.getNumOfChildren() && k2 < trace2.getNumOfChildren()
					&& trace1.getChild(k1).getID() == trace2.getChild(k2).getID()) {	
				// Merge corresponding node
				AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), w1, trace2.getChild(k2), w2, accumulate, scoreOnly);
				if (!scoreOnly) {
					mergedTrace.addChild(mergedChild);
				}
				startExclusive1 = trace1.getChild(k1).getTraceTime().getEndTimeInclusive();
				startInclusive1 = trace1.getChild(k1).getTraceTime().getEndTimeExclusive();
				startExclusive2 = trace2.getChild(k2).getTraceTime().getEndTimeInclusive();
				startInclusive2 = trace2.getChild(k2).getTraceTime().getEndTimeExclusive();
				
				// Compute the difference between the gaps among (k1-1, k1) in node1 and (k2-1, k2) in node2.
				double diff = computeRangeDiff(trace1.getMinGapDurationBeforeChild(k1) + gapDiffMin, trace1.getMaxGapDurationBeforeChild(k1) + gapDiffMax,
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
				
				if (compare == 0 && (trace1.getCFGGraph() == null || !trace1.getCFGGraph().valid))
					return mergeProfileNode(trace1, w1, trace2, w2, accumulate, scoreOnly);
				
				if (compare == 0) // increase k1 if k1 should be placed ahead of k2; increase k2 otherwise.
					compare = trace1.getCFGGraph().compareNodeOrder(trace1.getChild(k1), trace2.getChild(k2));

				if (compare < 0) { // increase k1
					startExclusive2 += trace1.getChild(k1).getTraceTime().getStartTimeExclusive() - startExclusive1;
					startInclusive2 += trace1.getChild(k1).getTraceTime().getStartTimeInclusive() - startInclusive1;
					startExclusive2 = Math.min(startExclusive2, 
							(k2 == trace2.getNumOfChildren()) ? trace2.getTraceTime().getEndTimeInclusive() : trace2.getChild(k2).getTraceTime().getStartTimeExclusive());
					startInclusive2 = Math.min(startInclusive2, 
							(k2 == trace2.getNumOfChildren()) ? trace2.getTraceTime().getEndTimeExclusive() : trace2.getChild(k2).getTraceTime().getStartTimeInclusive());
					
					AbstractTreeNode dummyNode = null;
					dummyNode = trace1.getChild(k1).voidDuplicate();

					TraceTimeStruct ts = new TraceTimeStruct();
					ts.setStartTimeExclusive(startExclusive2);
					ts.setStartTimeInclusive(startInclusive2);
					ts.setEndTimeInclusive(startExclusive2);
					ts.setEndTimeExclusive(startInclusive2);
					
					dummyNode.setTraceTime(ts);
					
					dummyNode.setWeight(trace2.getWeight());

					AbstractTreeNode mergedChild = mergeNode(trace1.getChild(k1), w1, dummyNode, w2, accumulate, scoreOnly);
					if (!scoreOnly) {
						mergedTrace.addChild(mergedChild);
					}
					startExclusive1 = trace1.getChild(k1).getTraceTime().getEndTimeInclusive();
					startInclusive1 = trace1.getChild(k1).getTraceTime().getEndTimeExclusive();
					
					// compute diff score
					gapDiffMin += trace1.getMinGapDurationBeforeChild(k1);
					gapDiffMax += trace1.getMaxGapDurationBeforeChild(k1);
					
					inclusiveDiff += mergedChild.getInclusiveDiffScore(); // belongs to 1.3) when not accumulating and 1) when accumulating

					k1++;
				}
				else {
					startExclusive1 += trace2.getChild(k2).getTraceTime().getStartTimeExclusive() - startExclusive2;
					startInclusive1 += trace2.getChild(k2).getTraceTime().getStartTimeInclusive() - startInclusive2;
					startExclusive1 = Math.min(startExclusive1, 
							(k1 == trace1.getNumOfChildren()) ? trace1.getTraceTime().getEndTimeInclusive() : trace1.getChild(k1).getTraceTime().getStartTimeExclusive());
					startInclusive1 = Math.min(startInclusive1, 
							(k1 == trace1.getNumOfChildren()) ? trace1.getTraceTime().getEndTimeExclusive() : trace1.getChild(k1).getTraceTime().getStartTimeInclusive());
										
					AbstractTreeNode dummyNode = null;
					dummyNode = trace2.getChild(k2).voidDuplicate();

					TraceTimeStruct ts = new TraceTimeStruct();
					ts.setStartTimeExclusive(startExclusive1);
					ts.setStartTimeInclusive(startInclusive1);
					ts.setEndTimeInclusive(startExclusive1);
					ts.setEndTimeExclusive(startInclusive1);
					
					dummyNode.setTraceTime(ts);
					
					dummyNode.setWeight(trace1.getWeight());
					
					AbstractTreeNode mergedChild = mergeNode(dummyNode, w1, trace2.getChild(k2), w2, accumulate, scoreOnly);
					if (!scoreOnly) {
						mergedTrace.addChild(mergedChild);
					}
					
					startExclusive2 = trace2.getChild(k2).getTraceTime().getEndTimeInclusive();
					startInclusive2 = trace2.getChild(k2).getTraceTime().getEndTimeExclusive();
					
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
		inclusiveDiff += (double)computeRangeDiff(gapDiffMin, gapDiffMax, 0, 0) * (double)w1 * w2; // belongs to 2.3)
		
		double exclusiveDiff = (double)computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
				trace2.getMinDuration(), trace2.getMaxDuration()) * (double)w1 * w2;
		
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
			inclusiveDiff = Math.max(inclusiveDiff, (double)computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
				trace2.getMinDuration(), trace2.getMaxDuration()) * (double)w1 * w2 + trace1.getInclusiveDiffScore() + trace2.getInclusiveDiffScore());
		else
			inclusiveDiff = Math.max(inclusiveDiff, (double)computeRangeDiff(trace1.getMinDuration(), trace1.getMaxDuration(),
					trace2.getMinDuration(), trace2.getMaxDuration()) * (double)w1 * w2);
		
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
	
	private ProfileNode mergeProfileNode(AbstractTreeNode node1, int weight1, AbstractTreeNode node2, int weight2, boolean accumulate, boolean scoreOnly) {
		ProfileNode prof1, prof2;
		if (node1 instanceof ProfileNode) prof1 = (ProfileNode) node1;
		else prof1 = ProfileNode.toProfile(node1);
		
		if (node2 instanceof ProfileNode) prof2 = (ProfileNode) node2;
		else prof2 = ProfileNode.toProfile(node2);
		
		int w1 = prof1.getWeight();
		int w2 = prof2.getWeight();
		
		ProfileNode mergedProfile = null;
		if (!scoreOnly) {
			mergedProfile = (ProfileNode) prof1.duplicate(); //TODO memory optimization
			mergedProfile.clearChildren();
			
			mergedProfile.setTraceTime(TraceTimeStruct.mergeTimeStruct(prof1.getTraceTime(), weight1, prof2.getTraceTime(), weight2));
			
			mergedProfile.setMaxDurationInclusive(TraceAnalysisUtils.computeWeightedAverage(prof1.getMaxDurationInclusive(), w1, 
					prof2.getMaxDurationInclusive(), w2));
			mergedProfile.setMinDurationInclusive(TraceAnalysisUtils.computeWeightedAverage(prof1.getMinDurationInclusive(), w1,
					prof2.getMinDurationInclusive(), w2));
			mergedProfile.setMaxDurationExclusive(TraceAnalysisUtils.computeWeightedAverage(prof1.getMaxDurationExclusive(), w1,
					prof2.getMaxDurationExclusive(), w2));
			mergedProfile.setMinDurationExclusive(TraceAnalysisUtils.computeWeightedAverage(prof1.getMinDurationExclusive(), w1,
					prof2.getMinDurationExclusive(), w2));
			
			mergedProfile.setWeight(w1 + w2);
			mergedProfile.setInclusiveDiffScore(prof1.getInclusiveDiffScore() + prof2.getInclusiveDiffScore());
			mergedProfile.setExclusiveDiffScore(prof1.getExclusiveDiffScore() + prof2.getExclusiveDiffScore());
			mergedProfile.setMaxDurationRep(Math.max(prof1.getMaxDurationRep(), prof2.getMaxDurationRep()));
			mergedProfile.setMinDurationRep(Math.min(prof1.getMinDurationRep(), prof2.getMinDurationRep()));
			mergedProfile.setTotalDurationRep(prof1.getTotalDurationRep() + prof2.getTotalDurationRep());
		} else 
			mergedProfile = (ProfileNode) prof1.voidDuplicate();
		
		/**
		 * When accumulating difference scores, the inclusive difference score of the merged profile consists of the following components --
		 *  1) the inclusive difference score of all children, which can be divided to:
		 *    1.1) the inclusive difference score of all children in prof1;
		 *    1.2) the inclusive difference score of all children in prof2;
		 *    1.3) the difference score of children between prof1 and prof2;
		 *  2) the difference of exclusive duration = exclusive diff score of merged children
		 *    2.1) exclusive diff score of prof1
		 *    2.2) exclusive diff score of prof2
		 *    2.3) the difference score of the exclusive duration between prof1 and prof2
		 *  
		 *  When not accumulating, only 1.3) and 2.3) needs to be added up.
		 */
		double inclusiveDiff = 0;
		
		HashMap<Integer, ProfileNode> map1 = prof1.getChildMap();
		HashMap<Integer, ProfileNode> map2 = prof2.getChildMap();
		
		for (ProfileNode child1 : map1.values()) {
			// Children that both profile have.
			if (map2.containsKey(child1.getID())) {
				ProfileNode child2 = map2.get(child1.getID());
				ProfileNode mergedChild = (ProfileNode) mergeNode(child1, w1, child2, w2, accumulate, scoreOnly);
				if (!scoreOnly) mergedProfile.addChild(mergedChild);
				inclusiveDiff += mergedChild.getInclusiveDiffScore(); // belongs to 1.3) when not accumulating and 1) when accumulating
			}
			// Children that only profile 1 has.
			else {
				ProfileNode child2 = (ProfileNode) child1.voidDuplicate();
				child2.setWeight(w2);
				ProfileNode mergedChild = (ProfileNode) mergeNode(child1, w1, child2, w2, accumulate, scoreOnly);
				if (!scoreOnly) mergedProfile.addChild(mergedChild);
				inclusiveDiff += mergedChild.getInclusiveDiffScore(); // belongs to 1.3) when not accumulating and 1) when accumulating
			}
		}
		
		for (ProfileNode child2 : map2.values())
			// Children that only profile 2 has.
			if (!map1.containsKey(child2.getID())) {
				ProfileNode child1 = (ProfileNode) child2.voidDuplicate();
				child1.setWeight(w1);
				ProfileNode mergedChild = (ProfileNode) mergeNode(child1, w1, child2, w2, accumulate, scoreOnly);
				if (!scoreOnly) mergedProfile.addChild(mergedChild);
				inclusiveDiff += mergedChild.getInclusiveDiffScore(); // belongs to 1.3) when not accumulating and 1) when accumulating
			}
		
		inclusiveDiff += (double)computeRangeDiff(prof1.getMinDurationExclusive(), prof1.getMaxDurationExclusive(), 
				prof2.getMinDurationExclusive(), prof2.getMaxDurationExclusive()) * (double)w1 * w2; // belongs to 2.3)
		
		double exclusiveDiff = (double)computeRangeDiff(prof1.getMinDurationInclusive(), prof1.getMaxDurationInclusive(), 
				prof2.getMinDurationInclusive(), prof2.getMaxDurationInclusive()) * (double)w1 * w2;
		
		if (accumulate) {
			exclusiveDiff += prof1.getExclusiveDiffScore() + prof2.getExclusiveDiffScore(); 
			
			// The above code has added 1) and 2.3) to inclusive diff score. Add 2.1) and 2.2) to inclusive diff score below.
			// Add 2.1)
			inclusiveDiff += prof1.getInclusiveDiffScore();
			for (ProfileNode child : prof1.getChildMap().values())
				inclusiveDiff -= child.getInclusiveDiffScore();
			
			// add 2.2)
			inclusiveDiff += prof2.getInclusiveDiffScore();
			for (ProfileNode child : prof2.getChildMap().values())
				inclusiveDiff -= child.getInclusiveDiffScore();
		}
		
		if (accumulate)
			inclusiveDiff = Math.max(inclusiveDiff, (double)computeRangeDiff(prof1.getMinDurationInclusive(), prof1.getMaxDurationInclusive(), 
				prof2.getMinDurationInclusive(), prof2.getMaxDurationInclusive()) * (double)w1 * w2 + prof1.getInclusiveDiffScore() + prof2.getInclusiveDiffScore());
		else
			inclusiveDiff = Math.max(inclusiveDiff, (double)computeRangeDiff(prof1.getMinDurationInclusive(), prof1.getMaxDurationInclusive(), 
				prof2.getMinDurationInclusive(), prof2.getMaxDurationInclusive()) * (double)w1 * w2);

		mergedProfile.setExclusiveDiffScore(exclusiveDiff);
		mergedProfile.setInclusiveDiffScore(inclusiveDiff);
		
		return mergedProfile;
	}
	
	private AbstractTreeNode mergeClusterNode(ClusterSetNode node1, int weight1, ClusterSetNode node2, int weight2, boolean accumulate, boolean scoreOnly) {
		// Adjust weight of rep for dummy node
		if (node1.getNumOfClusters() == 0)  
			node1.getRep().setWeight(node1.getWeight() * node2.getRep().getWeight());
		if (node2.getNumOfClusters() == 0)
			node2.getRep().setWeight(node2.getWeight() * node1.getRep().getWeight());
		
		// TODO Adjust weight of rep if two nodes have diff number of iterations
		
		Cluster[] cluster = new Cluster[node1.getNumOfClusters() + node2.getNumOfClusters()];
		
		//if (node1.getID() == 85162 && !scoreOnly)
		//	System.out.println();
		
		for (int i = 0; i < node1.getNumOfClusters(); i++)
			cluster[i] = (Cluster) node1.getCluster(i).duplicate();
		for (int i = 0; i < node2.getNumOfClusters(); i++)
			cluster[i+node1.getNumOfClusters()] = (Cluster) node2.getCluster(i).duplicate();
		
		if (!scoreOnly) {
			int numMembers = 0;
			for (Cluster c : cluster) 
				numMembers += c.getWeight();
			
			cluster = mergeCluster(cluster, maxNumOfClusters(numMembers), 1);
		}
		
		AbstractTreeNode mergedNode = null;
		if (cluster == null) mergedNode = mergeProfileNode(node1, weight1, node2, weight2, accumulate, scoreOnly);
		else {
			AbstractTreeNode rep = null;
			if (!scoreOnly) {
				rep = mergeNode(node1.getRep(), node1.getRep().getWeight(), node2.getRep(), node2.getRep().getWeight(), false, false);
				rep.clearDiffScore();
			} else
				rep = cluster[0].getRep().voidDuplicate();
			
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
			
			/*if (clusterDebug && node1.getID() == clusterDebugID) {
				System.err.println("Merge start: ");
			}*/
				
			AbstractTreeNode[][] diffTreeNode = new AbstractTreeNode[numCluster1][numCluster2];
			double[][] diffRatio = new double[numCluster1][numCluster2];
			for (int i = 0; i < numCluster1; i++)
				for (int j = 0; j < numCluster2; j++) {
				//	if (node1.getID() == 36268 && node1.getWeight() == 2 && node2.getWeight() == 2 && i == j) {
				//	    System.out.println("computing");
				//	}
					//diffTreeNode[i][j] = mergeNode(rep1[i], rep1[i].getWeight(), rep2[j], rep2[j].getWeight(), false);
					//diffRatio[i][j] = diffTreeNode[i][j].getInclusiveDiffScore() / (double)(rep1[i].getWeight() * rep2[j].getWeight()) / (double)(rep1[i].getDuration() + rep2[j].getDuration());
					diffTreeNode[i][j] = mergeNode(rep1[i], rep1[i].getWeight(), rep2[j], rep2[j].getWeight(), false, true);
					diffRatio[i][j] = diffTreeNode[i][j].getInclusiveDiffScore() / (double)(rep1[i].getWeight() * rep2[j].getWeight()) / (double)(rep1[i].getDuration() + rep2[j].getDuration());
				}

			/*if (clusterDebug && node1.getID() == clusterDebugID) {
				System.err.println("Merge: ");
				for (int i = 0; i < numCluster1; i++) {
					for (int j = 0; j < numCluster2; j++) {
						System.err.print(diffTreeNode[i][j].getInclusiveDiffScore() / (rep1[i].getWeight() * rep2[j].getWeight()) + "  ");
					}
					System.err.println();
				}
				
				System.err.println(rep1[0].getWeight() + " " + rep2[0].getWeight());
				clusterDebug = false;
			}*/
			
			while (numCluster1 > 0 && numCluster2 > 0) {
				int idx1 = 0, idx2 = 0;
				for (int i = 0; i < numCluster1; i++)
					for (int j = 0; j < numCluster2; j++) 
						if (diffRatio[i][j] < diffRatio[idx1][idx2]) {
							idx1 = i;
							idx2 = j;
						}
				
				int numMatched = Math.min(numMember1[idx1], numMember2[idx2]);

				if (!scoreOnly) 
					diffTreeNode[idx1][idx2] = mergeNode(rep1[idx1], rep1[idx1].getWeight(), rep2[idx2], rep2[idx2].getWeight(), false, scoreOnly);
				diffTreeNode[idx1][idx2].stretchDiffScore(numMatched, rep1[idx1].getWeight() * rep2[idx2].getWeight());
				addDiffScore(rep, diffTreeNode[idx1][idx2]);
				
	/*			if (clusterDebug && node1.getID() == clusterDebugID) {
					System.out.println("Diff = " + rep.getInclusiveDiffScore() + " after added " + diffTreeNode[idx1][idx2].getInclusiveDiffScore()
							+ "@" + diffRatio[idx1][idx1]);
					System.out.println("matching " + rep1[idx1].getName() + 
							" with " + rep2[idx2].getName());
				}*/
				
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
				AbstractTreeNode diff = mergeNode(rep1[numCluster1], rep1[numCluster1].getWeight(), dummyNode, 1, false, scoreOnly);
				diff.stretchDiffScore(numMember1[numCluster1], rep1[numCluster1].getWeight());
				addDiffScore(rep, diff);
			}
			
			//if (clusterDebug && node1.getID() == clusterDebugID) {
			//	System.err.println("diff = " + rep.getInclusiveDiffScore());
			//}

			while (numCluster2 > 0) {
				numCluster2 --;
				AbstractTreeNode dummyNode = rep2[numCluster2].voidDuplicate();
				dummyNode.setWeight(1);
				AbstractTreeNode diff = mergeNode(dummyNode, 1, rep2[numCluster2], rep2[numCluster2].getWeight(), false, scoreOnly);
				diff.stretchDiffScore(numMember2[numCluster2], rep2[numCluster2].getWeight());
				addDiffScore(rep, diff);
			}
			
			if (clusterDebug && node1.getID() == clusterDebugID) {
				System.out.println("*************************FINISHED*****************************");
			}

			//if (clusterDebug && node1.getID() == clusterDebugID) {
			//	System.err.println("diff = " + rep.getInclusiveDiffScore());
			//}
			
			/*long diff = computeClusterDiff(node1, node2, false);
			if (rep.getInclusiveDiffScore() / (node1.getWeight() * node2.getWeight()) != diff) {
				System.err.println("Different result for Cluster " + node1.getID() + ": " + diff + " vs " + rep.getInclusiveDiffScore() / (node1.getWeight() * node2.getWeight()));
				//System.err.println(node1.getCluster(0).toString(node1.getDepth()+3, 0));
				//System.err.println(node2.getCluster(0).toString(node2.getDepth()+3, 0));
			}
			*/
			if (accumulate) {
				addDiffScore(rep, node1.getRep());
				addDiffScore(rep, node2.getRep());
			}
			
			if (!scoreOnly)
				mergedNode = new ClusterSetNode(node1, node2, rep, cluster);
			else 
				mergedNode = rep;
		}

		return mergedNode;
	}
	
	private boolean clusterDebug = false;
    private int clusterDebugID = 36268;
	
	public AbstractTreeNode mergeNode(AbstractTreeNode node1, int weight1, AbstractTreeNode node2, int weight2, boolean accumulate, boolean scoreOnly) {
		if (node1.getWeight() != weight1) {
			System.err.println(node1.getWeight() + " vs " + weight1 + " : " + node1.toString(0, 0, 0));
		}
		
		if (node2.getWeight() != weight2) {
			System.err.println(node2.getWeight() + " vs " + weight2 + " : " + node2.toString(0, 0, 0));
		}
		
		if (node1.getDepth() != node2.getDepth()) {
			System.err.println("Merging node at different depth.");
		}
		
		if ((node1 instanceof AbstractTraceNode) && (node2 instanceof AbstractTraceNode))
			return mergeTraceNode((AbstractTraceNode)node1, weight1, (AbstractTraceNode)node2, weight2, accumulate, scoreOnly);
		else if ((node1 instanceof IterationTrace) && (node2 instanceof IterationTrace))
			return mergeTraceNode((AbstractTraceNode)node1, weight1, (AbstractTraceNode)node2, weight2, accumulate, scoreOnly);
		else if ((node1 instanceof ClusterSetNode) && (node2 instanceof ClusterSetNode))
			return mergeClusterNode((ClusterSetNode)node1, weight1, (ClusterSetNode)node2, weight2, accumulate, scoreOnly);
		else 
			return mergeProfileNode(node1, weight1, node2, weight2, accumulate, scoreOnly);
		//TODO add a case for TraceNode vs ClusterNode
	}
	
	class ComputeDiffThread implements Callable<double[]> {
		final Cluster[] cluster;
		int n, i, j;
		final int start;
		final int end;
		double[] diff;
		
		public ComputeDiffThread(Cluster[] cluster, int start, int end) {
			this.cluster = cluster;
			this.start = start;
			this.end = end;
			this.diff = new double[end-start];
			
			computeStartIndex();
		}
		
		private void computeStartIndex() {
			n = cluster.length;
			i = 0;
			
			/**
			 * Given 2D index (i,j), 1D index t, and num of cluster N, we have
			 * t = (N-1)(N-2)/2 - (N-1-i)(N-2-i)/2 + (j-1);
			 */
			while ( ((n-1)*(n-2)/2 - (n-1-i)*(n-2-i)/2 + (n-2)) < start) 
				i++;
			
			j = start - (n-1)*(n-2)/2 + (n-1-i)*(n-2-i)/2 + 1;
		}
		
		@Override
		public double[] call() throws Exception {
			int count = 0;
			while (count < end - start) {
				diff[count] = (double)mergeNode(cluster[i].getRep(), cluster[i].getRep().getWeight(),
						cluster[j].getRep(), cluster[j].getRep().getWeight(), false, true).getInclusiveDiffScore()
						/ cluster[i].getRep().getWeight() / cluster[j].getRep().getWeight() 
						/ (double)(cluster[i].getRep().getDuration() + cluster[j].getRep().getDuration());
				j++;
				if (j >= n) {
					i++;
					j=i+1;
				}
				
				count++;
			}	
			return diff;
		}
		
	}
	
	
	private Cluster[] mergeCluster(Cluster[] cluster, int maxNumCluster, int numProc) {
		int numCluster = cluster.length;
		double[][] diff = new double[numCluster][numCluster];
		
		boolean parallelized = false;
		if (numProc > 1) {
			int workLoad = numCluster * (numCluster-1) / 2;
			if (numProc > workLoad) numProc = workLoad;
			
			ExecutorService threadExecutor = Executors.newFixedThreadPool(numProc); 
			ArrayList<Future<double[]>> futures = new ArrayList<Future<double[]>>();

			for (int p = 0; p < numProc; p++) {
				ComputeDiffThread thread = new ComputeDiffThread(cluster, workLoad*p/numProc, workLoad*(p+1)/numProc);
				Future<double[]> future = threadExecutor.submit(thread);
				futures.add(future);
			}
			
			int i = 0, j = 1;
			parallelized = true;
			for (Future<double[]> future : futures) {
				try {
					double[] temp = future.get();
					for (int k = 0; k < temp.length; k++) {
						diff[i][j] = temp[k];
						
						j++;
						if (j >= numCluster) {
							i++;
							j=i+1;
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					parallelized = false;
					break;
				} catch (ExecutionException e) {
					e.printStackTrace();
					parallelized = false;
					break;
				}
			}
			
			threadExecutor.shutdown();
		}
		
		if (!parallelized)
			for (int i = 0; i < numCluster; i++)
				for (int j = i+1; j < numCluster; j++) 
						diff[i][j] = (double)mergeNode(cluster[i].getRep(), cluster[i].getRep().getWeight(),
								cluster[j].getRep(), cluster[j].getRep().getWeight(), false, true).getInclusiveDiffScore()
								/ cluster[i].getRep().getWeight() / cluster[j].getRep().getWeight() 
								/ (double)(cluster[i].getRep().getDuration() + cluster[j].getRep().getDuration());
		
		double minDiff = 1;
		double maxDiff = 0;
		int idx1 = 1, idx2 = 2;
		
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
					  cluster[idx2].getRep(), cluster[idx2].getWeight(), true, false);

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
				diff[i][idx1] = (double)mergeNode(cluster[i].getRep(), cluster[i].getRep().getWeight(),
						cluster[idx1].getRep(), cluster[idx1].getRep().getWeight(), false, true).getInclusiveDiffScore()
						/ cluster[i].getRep().getWeight() / cluster[idx1].getRep().getWeight() 
						/ (double)(cluster[i].getRep().getDuration() + cluster[idx1].getRep().getDuration());
				
			//(double)computeDiff(cluster[i].getRep(), cluster[idx1].getRep(), false)
			//		/ (double)(cluster[i].getRep().getDuration() + cluster[idx1].getRep().getDuration());
			for (int i = idx1+1; i < numCluster; i++)
				diff[idx1][i] = (double)mergeNode(cluster[i].getRep(), cluster[i].getRep().getWeight(),
						cluster[idx1].getRep(), cluster[idx1].getRep().getWeight(), false, true).getInclusiveDiffScore()
						/ cluster[i].getRep().getWeight() / cluster[idx1].getRep().getWeight() 
						/ (double)(cluster[i].getRep().getDuration() + cluster[idx1].getRep().getDuration());
				
				//(double)computeDiff(cluster[idx1].getRep(), cluster[i].getRep(), false)
					//	/ (double)(cluster[idx1].getRep().getDuration() + cluster[i].getRep().getDuration());
			
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
	
	private AbstractTreeNode computeClusterRep(Cluster[] cluster, int numProc) {
		AbstractTreeNode node = cluster[0].getRep().duplicate();
		for (int i = 1; i < cluster.length; i++)
			node = mergeNode(node, node.getWeight(), cluster[i].getRep(), cluster[i].getWeight(), false, false);
		node.clearDiffScore();
		
		for (int i = 0; i < cluster.length; i++)
			addDiffScore(node, cluster[i].getRep());
		
		for (int i = 0; i < cluster.length; i++)
			for (int j = i+1; j < cluster.length; j++) {
				AbstractTreeNode diff = mergeNode(cluster[i].getRep(), cluster[i].getWeight(), cluster[j].getRep(), cluster[j].getWeight(), false, false);
				addDiffScore(node, diff);
			}
		return node;
	}
	
	private void labelCluster(AbstractTreeNode node, int ID) {
		if (node instanceof ClusterSetNode)
			((ClusterSetNode) node).addLabel(ID);
		else if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++)
				labelCluster(trace.getChild(i), ID);
		}
	}
	
	class FindClusterThread implements Callable<Cluster[]> {
		private final AbstractTraceNode loop;
		private final int begin;
		private final int end;
		private final int maxNumCluster;
		private final int numProc;
		
		public FindClusterThread(AbstractTraceNode loop, int begin, int end,
				int maxNumCluster, int numProc) {
			this.loop = loop;
			this.begin = begin;
			this.end = end;
			this.maxNumCluster = maxNumCluster;
			this.numProc = numProc;
		}

		@Override
		public Cluster[] call() {
			if (begin == end) {
				AbstractTreeNode node = loop.getChild(begin);
				
				if (node instanceof ShadowTraceTree)
					node = ((ShadowTraceTree) node).getRootTrace();
				else
					node = node.duplicate();
				
				labelCluster(node, begin);
				
				Cluster[] cluster = new Cluster[1];
				cluster[0] = new Cluster(node, begin);
				return cluster;
			}
			
			int mid = (begin+end)/2;
			Cluster[] cluster1 = null;
			Cluster[] cluster2 = null;
			
			boolean parallelized = false;
			if (numProc > 1) {
				ExecutorService threadExecutor = Executors.newFixedThreadPool(2); 
				
				FindClusterThread thread1 = new FindClusterThread(loop, begin, mid, maxNumCluster, numProc/2);
				Future<Cluster[]> future1 = threadExecutor.submit(thread1);
				
				FindClusterThread thread2 = new FindClusterThread(loop, mid+1, end, maxNumCluster, numProc/2);
				Future<Cluster[]> future2 = threadExecutor.submit(thread2);

				try {
					cluster1 = future1.get();
					cluster2 = future2.get();
					parallelized = true;
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
				threadExecutor.shutdown();			
			}

			if (!parallelized) {
				FindClusterThread thread = new FindClusterThread(loop, begin, mid, maxNumCluster, numProc);
				cluster1 = thread.call();
				thread = new FindClusterThread(loop, mid+1, end, maxNumCluster, numProc);
				cluster2 = thread.call();
			}
			
			if (cluster1 == null || cluster2 == null) return null;
			
			Cluster[] cluster = new Cluster[cluster1.length + cluster2.length];
			for (int i = 0; i < cluster1.length; i++)
				cluster[i] = cluster1[i];
			for (int i = 0; i < cluster2.length; i++)
				cluster[i+cluster1.length] = cluster2[i];
			
			cluster = mergeCluster(cluster, maxNumCluster, numProc);
//System.out.println("Finish merge from " + begin + " to " + end + ". # clusters = " + cluster.length + ".");			
			return cluster;
		}
	}
	
	/*
	private Cluster[] findCluster(AbstractTraceNode loop, int begin, int end, int maxNumCluster) {
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
	*/
	
	public AbstractTreeNode findCluster(AbstractTraceNode loop, int numProc) {
		//AbstractTraceNode noDiffLoop = (AbstractTraceNode) loop.duplicate();
		//noDiffLoop.clearDiffScore();
		
		FindClusterThread thread = new FindClusterThread(loop, 0, loop.getNumOfChildren()-1, 
				maxNumOfClusters(loop.getNumOfChildren()), numProc);
		Cluster[] cluster = thread.call();

		if (cluster == null) {
			AbstractTreeNode ret = ProfileNode.toProfile(loop);
			ret.clearDiffScore();
			return ret;
		}
		else if (this.rankName != null) 
			return new ClusterSetNode(loop, "data\\P" + this.rankName + "_C" + this.clusterCount++, computeClusterRep(cluster, numProc), cluster);
		else 
			return new ClusterSetNode(loop, "data\\AllProcs", computeClusterRep(cluster, numProc), cluster);
	}
	
	public AbstractTreeNode clusterLoops(AbstractTreeNode node) {
		if (!(node instanceof AbstractTraceNode)) return null;
		AbstractTraceNode trace = (AbstractTraceNode) node;
			
		for (int i = 0; i < trace.getNumOfChildren(); i++) {
			AbstractTreeNode newNode = clusterLoops(trace.getChild(i));
			if (newNode != null) trace.updateChild(i, newNode);
		}
		
		if (trace instanceof IteratedLoopTrace) {
			return findCluster(trace, 1);
			
			/*if (trace.getID() == 69617 || trace.getID() == 23299) {
				if (cluster != null) 
					System.out.print(cluster.print(cluster.getDepth()+1, 0));
				else
					System.out.println(trace.getName()+"("+trace.getID()+") not clustered.");
				System.out.println("-----------------------------------");
			}*/
			
			//if (cluster != null)
			//	System.out.println(trace.getName() + "(" + trace.getID() + "): " + ClusterIdentifier.computeDiff(trace, cluster));
			
			/*
			if (trace.getID() == 69932) {
				System.out.println("---------------------------------------------");
				System.out.println("TRACE: ");
				//trace = ((IteratedLoop)trace).rawLoop;
				System.out.print(trace.print(trace.getDepth(), 0));
				System.out.print(ProfileNode.toProfile(trace).print(trace.getDepth()+1, 0));
				System.out.println("CLUSTER: ");
				System.out.print(cluster.print(cluster.getDepth(), 0));
				System.out.print(ProfileNode.toProfile(cluster).print(cluster.getDepth()+1, 0));
			}*/
		}
		
		return null;
	}
	
	public void testDiff(AbstractTreeNode node) {
		//if (node.getID() == 0) {
		if (node instanceof IteratedLoopTrace) {
			AbstractTraceNode loop = (AbstractTraceNode)node;
			if (loop.getID() == 7159 || loop.getID() == 1598 || loop.getID() == 36268) {
				System.out.println(loop.getName()+"("+loop.getID()+")");
				System.out.print(loop.toString(loop.getDepth(), 0, 0));
				for (int i = 0; i < loop.getNumOfChildren(); i++) {
					for (int j = 0; j < loop.getNumOfChildren(); j++) {
						AbstractTreeNode diffNode = mergeNode(loop.getChild(i), 1, loop.getChild(j), 1, false, true);
						double diff = diffNode.getInclusiveDiffScore();
						//boolean test = (diff != computeDiff(loop.getChild(i), loop.getChild(j), false));
						diff = diff / (loop.getChild(i).getDuration() + loop.getChild(j).getDuration()) * 100;
						System.out.print("\t" + String.format("%.2f", diff) + "%");
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
