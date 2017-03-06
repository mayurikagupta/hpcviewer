package edu.rice.cs.hpc.traceAnalysis.iteration;

import java.util.HashMap;

import edu.rice.cs.hpc.traceAnalysis.data.AbstractLoop;
import edu.rice.cs.hpc.traceAnalysis.data.AbstractTraceTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.RawLoopWithIteration;
import edu.rice.cs.hpc.traceAnalysis.data.TraceTree;

public class IterationClassifier {
	final public static int detectionCutoffRatio = LoopDetector.detectionCutoffRatio;
	final public int detectionCutoff;
	final public TraceTree traceTree;
	
	public IterationClassifier(TraceTree traceTree) {
		this.traceTree = traceTree;
		this.detectionCutoff = this.traceTree.sampleFrequency * detectionCutoffRatio;
	}
	
	private HashMap<Integer, Integer> getOccurrenceMap(AbstractTraceTreeNode node) {
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (int i = 0; i < node.getNumOfChildren(); i++) 
			map.put(node.getChild(i).getID(), i);
		return map;
	}
	
	private long getMinGapDuration(AbstractTraceTreeNode node, int childIdx) {
		long duration;
		if (childIdx == 0) duration = node.getChild(childIdx).getStartTimeExclusive() - node.getStartTimeInclusive() + 1;
		else duration = node.getChild(childIdx).getStartTimeExclusive() - node.getChild(childIdx-1).getEndTimeExclusive() + 1;
		
		if (duration < 0) duration = 0;
		return duration;
	}
	
	private long getMaxGapDuration(AbstractTraceTreeNode node, int childIdx) {
		long duration;
		if (childIdx == 0) duration = node.getChild(childIdx).getStartTimeInclusive() - node.getStartTimeExclusive() - 1;
		else duration = node.getChild(childIdx).getStartTimeInclusive() - node.getChild(childIdx-1).getEndTimeInclusive() - 1;
		
		if (duration < 0) duration = 0;
		return duration;
	}
	
	private long computeRangeDiff(long min1, long max1, long min2, long max2) {
		if (max2 < min1) return min1 - max2;
		if (max1 < min2) return min2 - max1;
		return 0;
	}
	
	private long computeDiff(AbstractTraceTreeNode node1, AbstractTraceTreeNode node2) {
		HashMap<Integer, Integer> map1 = getOccurrenceMap(node1);
		HashMap<Integer, Integer> map2 = getOccurrenceMap(node2);
		
		int k1 = 0;
		int k2 = 0;
		
		long diff = 0;
		long gapDiff = 0;
		
		while (k1 < node1.getNumOfChildren() && k2 < node2.getNumOfChildren()) {
			// At the same child 
			if (node1.getChild(k1).getID() == node2.getChild(k2).getID()) {
				// Compute the difference between the gaps among (k1-1, k1) in node1 and (k2-1, k2) in node2.
				diff += computeRangeDiff(getMinGapDuration(node1, k1) - gapDiff, getMaxGapDuration(node1, k1) - gapDiff,
							getMinGapDuration(node2, k2), getMaxGapDuration(node2, k2));
				gapDiff = 0;
				
				/*
				// Compute the different between two children.
				// When both nodes are no less than detectionCutoff, explore them in detail.
				if (node1.getChild(k1).getDuration() >= detectionCutoff && node2.getChild(k2).getDuration() >= detectionCutoff
						//TODO should be able to go into loops in the future
						&& !(node1.getChild(k1) instanceof AbstractLoop))
					diff += computeDiff(node1.getChild(k1), node2.getChild(k2));
				// If not, use their duration to compute difference
				else*/
					diff += computeRangeDiff(node1.getChild(k1).getMinDuration(), node1.getChild(k1).getMaxDuration(),
							node2.getChild(k2).getMinDuration(), node2.getChild(k2).getMaxDuration());
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
				gapDiff -= getMinGapDuration(node2, k2) + node2.getChild(k2).getStartTimeInclusive() - node2.getChild(k2).getStartTimeExclusive();
				diff += node2.getChild(k2).getMinDuration();
				k2++;
			} else { // Advance k1 in other cases 
				gapDiff += getMinGapDuration(node1, k1) + node1.getChild(k1).getStartTimeInclusive() - node1.getChild(k1).getStartTimeExclusive();
				diff += node1.getChild(k1).getMinDuration();
				k1++;
			}
		}
		
		while (k1 < node1.getNumOfChildren()) {
			gapDiff += getMinGapDuration(node1, k1) + node1.getChild(k1).getStartTimeInclusive() - node1.getChild(k1).getStartTimeExclusive();
			diff += node1.getChild(k1).getMinDuration();
			k1++;
		}
		while (k2 < node2.getNumOfChildren()) {
			gapDiff -= getMinGapDuration(node2, k2) + node2.getChild(k2).getStartTimeInclusive() - node2.getChild(k2).getStartTimeExclusive();
			diff += node2.getChild(k2).getMinDuration();
			k2++;
		}
		gapDiff += node1.getEndTimeExclusive() - node1.getChild(k1-1).getEndTimeExclusive();
		gapDiff -= node2.getEndTimeExclusive() - node2.getChild(k2-1).getEndTimeExclusive();
		diff += Math.abs(gapDiff);
		
		return diff;
	}
	
	public void testDiff(AbstractTraceTreeNode node) {
		if (node instanceof RawLoopWithIteration) {
			RawLoopWithIteration loop = (RawLoopWithIteration)node;
			//System.out.println(loop.getName()+"("+loop.getID()+")");\
			System.out.print(loop.print(loop.getDepth()));
			for (int i = 0; i < loop.getNumOfChildren(); i++) {
				for (int j = 0; j < loop.getNumOfChildren(); j++) {
					long diff = computeDiff(loop.getChild(i), loop.getChild(j));
					diff = diff * 1000 / (loop.getChild(i).getDuration() + loop.getChild(j).getDuration());
					System.out.print("\t" + diff/10 + "." + diff%10);
				}
				System.out.println();
			}
			System.out.println();
			testDiff(loop.getChild(loop.getNumOfChildren()-1));
		}
		else 
			for (int i = 0; i < node.getNumOfChildren(); i++) 
			testDiff(node.getChild(i));
	}
}
