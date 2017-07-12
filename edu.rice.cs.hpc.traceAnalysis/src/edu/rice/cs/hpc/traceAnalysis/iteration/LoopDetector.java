package edu.rice.cs.hpc.traceAnalysis.iteration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.FunctionTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IteratedLoopTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IterationTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ProfileNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.RawLoopTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTimeStruct;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTree;
import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

public class LoopDetector {
	final public static int noiseUpperCutoffRatio = 10;
	final public static int noiseLowerCutoffRatio = 2;
	
	final public int detectionCutoff;
	final public int noiseUpperCutoff;
	final public int noiseLowerCutoff;
	
	final public TraceTree traceTree;
	
	public int detectedLoopID = 0;
	
	public LoopDetector(TraceTree traceTree) {
		this.traceTree = traceTree;
		this.detectionCutoff = this.traceTree.sampleFrequency * TraceAnalysisUtils.traceCutoffMultiplier;
		this.noiseUpperCutoff = this.traceTree.sampleFrequency * noiseUpperCutoffRatio;
		this.noiseLowerCutoff = this.traceTree.sampleFrequency * noiseLowerCutoffRatio;
	}
	
	// fully tested
    private OccurrenceRecord[] getOccurrenceRecord(AbstractTraceNode node, int startChild/*inclusive*/, int endChild/*exclusive*/) {
    	// ID -> Occurrence Record
        HashMap<Integer, OccurrenceRecord> map = new HashMap<Integer, OccurrenceRecord>();
        
        Vector<OccurrenceRecord> records = new Vector<OccurrenceRecord>();
        int predID = -1;
        for (int k = startChild; k < endChild; k++) {
        	int curID = node.getChild(k).getID();
        	if (!map.containsKey(curID)) {
        		OccurrenceRecord record = new OccurrenceRecord(curID, k);
        		if (node.getChild(k) instanceof RawLoopTrace)
        			record.minDuration = 0;
        		records.add(record);
        		map.put(curID, record);
        	}
        	OccurrenceRecord record = map.get(curID);
        	record.lastOccur = k;
        	record.occurrence++;
        	record.totalDuration += node.getChild(k).getDuration();
        	record.minDuration = Math.min(record.minDuration, node.getChild(k).getDuration());
        	if (predID != -1) {
        		record.pred.add(predID);
        		map.get(predID).post.add(curID);
        	}
        	predID = curID;
        }
        
        OccurrenceRecord[] array = records.toArray(new OccurrenceRecord[0]);

        return array;
    }
    
    /*
     * Derive a frontier, which consists of a set of CCT nodes that indicates the start or end of an iteration in the loop.
     * To filter the noise introduced by sampling, every instance of the CCT node in the frontier must has a number of samples of more than an adaptively adjusted threshold.
     * @param rootID ID of the CCT node that indicates the start or end of the loop.
     * @param map a map that maps all CCT IDs in the loop to their corresponding occurrence record.
     * @param isBeginFrontier TRUE if deriving a begin frontier, FALSE if deriving an end frontier.
     * @return
     */
    /*
    // tested and generated expecting output
    private HashSet<Integer> getFrontier(int rootID, HashMap<Integer, OccurrenceRecord> map, boolean isBeginFrontier) {
		HashSet<Integer> frontier = new HashSet<Integer>();
		HashSet<Integer> set = new HashSet<Integer>();
		
		frontier.add(rootID);
		set.add(rootID);

		int noiseCufoff = noiseLowerCutoff; // The threshold should be no less than a lower cutoff.
		while (noiseCufoff <= noiseUpperCutoff) { // Try to increase the threshold until reaching an upper cutoff.
    		LinkedList<Integer> newAdded = new LinkedList<Integer>();
    		newAdded.addAll(frontier);
    		
    		while (newAdded.size() > 0) {
    			int cctID = newAdded.poll();
    			if (map.containsKey(cctID) && map.get(cctID).minDuration <= noiseCufoff) 
    				for (int id : (isBeginFrontier ? map.get(cctID).post : map.get(cctID).pred)) 
    					if (!set.contains(id) && map.containsKey(id)) {
    						set.add(id);
    						newAdded.add(id);
    					}
    		}
    		
    		LinkedList<Integer> newFrontier = new LinkedList<Integer>();
    		newFrontier.addAll(set);
    		for (int m = newFrontier.size(); m>0; m--){
    			int cctID = newFrontier.poll();
    			if (map.get(cctID).minDuration > noiseCufoff)
    				newFrontier.add(cctID);
    		}
    		
    		if (newFrontier.size() > 0) {
    			frontier.clear();
    			frontier.addAll(newFrontier);
    			noiseCufoff = Integer.MAX_VALUE;
    			for (int cctID: frontier)
    				if (map.get(cctID).minDuration < noiseCufoff)
    					noiseCufoff = (int) map.get(cctID).minDuration;
    		}
    		else break;
		}
		
		if (noiseCufoff == noiseLowerCutoff) frontier.clear();
    	return frontier;
    }

    
    // tested and generated expecting output
    private IteratedLoop detectIterations(RawLoopTrace rawLoop)  {
    	// Get occurrence records ranked by their first occurred child index.
    	OccurrenceRecord[] records = getOccurrenceRecord(rawLoop, 0, rawLoop.getNumOfChildren());
    	if (records.length == rawLoop.getNumOfChildren()) return null;
    	
		// Try to identify iteration boundaries.
		// TODO we assume no if-statement in the loop at this moment.

		HashMap<Integer, OccurrenceRecord> map = new HashMap<Integer, OccurrenceRecord>();
		for (int i = 0; i < records.length; i++) 
			map.put(records[i].ID, records[i]);
		
		// Derive a begin frontier, which consists of a set of CCT nodes that indicates the start of an iteration.
		// Every instance of the CCT node in the frontier has a duration of more than an adaptively adjusted threshold.
		int startIndex = 0;
		while (map.get(rawLoop.getChild(startIndex).getID()).occurrence == 1) startIndex++;
		HashSet<Integer> beginFrontier = getFrontier(rawLoop.getChild(startIndex).getID(), map, true);
		int beginNoiseCutoff = beginFrontier.size() > 0 ? Integer.MAX_VALUE : 0;
		for (int cctID: beginFrontier)
			if (map.get(cctID).minDuration < beginNoiseCutoff)
				beginNoiseCutoff = (int) map.get(cctID).minDuration;
		
		int endIndex = rawLoop.getNumOfChildren()-1;
		while (map.get(rawLoop.getChild(endIndex).getID()).occurrence == 1) endIndex--;
		HashSet<Integer> endFrontier = getFrontier(rawLoop.getChild(endIndex).getID(), map, false);
		int endNoiseCutoff = endFrontier.size() > 0 ? Integer.MAX_VALUE : 0;
		for (int cctID: endFrontier)
			if (map.get(cctID).minDuration < endNoiseCutoff)
				endNoiseCutoff = (int) map.get(cctID).minDuration;
		
		boolean isBeginFrontier;
		// Both frontiers are good, choose the one with less element.
		if (Math.min(beginNoiseCutoff, endNoiseCutoff) > noiseUpperCutoff) { 
			if (beginFrontier.size() < endFrontier.size()) isBeginFrontier = true;
			else isBeginFrontier = false;
		}
		// Only one of the frontiers are good, choose it.
		else if (beginNoiseCutoff > noiseUpperCutoff) isBeginFrontier = true; 
		else if (endNoiseCutoff > noiseUpperCutoff) isBeginFrontier = false;
		// None of them are good enough. Choose the one with larger noise cutoff.
		// If cutoffs are same, choose the one with less element.
		else 
			if (beginNoiseCutoff > endNoiseCutoff) isBeginFrontier = true;
			else if (beginNoiseCutoff < endNoiseCutoff) isBeginFrontier = false;
			else if (beginFrontier.size() < endFrontier.size()) isBeginFrontier = true;
			else isBeginFrontier = false;
		
		HashSet<Integer> frontier = isBeginFrontier ? beginFrontier : endFrontier;

		if (frontier.isEmpty()) return null;
		
		IteratedLoop retLoop = new IteratedLoop(rawLoop);
		
		//TODO assign appropriate iteration ID
		Iteration iter = new Iteration(retLoop, retLoop.getNumOfChildren());
		long startTimeExclusive = rawLoop.getTime().getStartTimeExclusive();
	    long startTimeInclusive = rawLoop.getTime().getStartTimeInclusive();
		
		//TODO we assume that two neighboring frontier occurrences are separated by other functions.
		boolean inFrontier = false;
		int lastFrontierID = 0;
		for (int i = 0; i < rawLoop.getNumOfChildren(); i++) {
			if (!isBeginFrontier) // If end frontier, add the current child before terminating this iteration
				iter.addChild(rawLoop.getChild(i), rawLoop.getChildTime(i), rawLoop.getChildCFGNode(i));
			
			// determine if the iteration should be terminated.
			if (frontier.contains(rawLoop.getChild(i).getID())) {
				// TODO a patch so that if the same frontier function occurs twice, they will be put into different iterations.
				if (rawLoop.getChild(i).getID() == lastFrontierID) inFrontier = false;
					
				if (!inFrontier) 
					if (iter.getNumOfChildren() > 0) {
						// start time for the current iteration
						iter.getTime().setStartTimeExclusive(startTimeExclusive);
						iter.getTime().setStartTimeInclusive(startTimeInclusive);
						// start time for the next iteration
						if (isBeginFrontier) {
							startTimeExclusive = rawLoop.getChildTime(i).getStartTimeExclusive();
							startTimeInclusive = rawLoop.getChildTime(i).getStartTimeInclusive();
						}
						else {
							startTimeExclusive = rawLoop.getChildTime(i).getEndTimeInclusive();
							startTimeInclusive = rawLoop.getChildTime(i).getEndTimeExclusive();
						}
						// end time for the current iteration
						iter.getTime().setEndTimeExclusive(startTimeInclusive);
						iter.getTime().setEndTimeInclusive(startTimeExclusive);

						retLoop.addChild(iter, iter.getTime(), null);
						iter.setDepth(retLoop.getDepth()+1);
						iter = new Iteration(retLoop, retLoop.getNumOfChildren());
					}
				inFrontier = true;
				lastFrontierID = rawLoop.getChild(i).getID();
			}
			else {
				inFrontier = false;
			}
			
			if (isBeginFrontier) // if begin frontier, add the current child after terminating the previous iteration
				iter.addChild(rawLoop.getChild(i), rawLoop.getChildTime(i), rawLoop.getChildCFGNode(i));
		}
		
		if (iter.getNumOfChildren() > 0) {
			iter.getTime().setStartTimeExclusive(startTimeExclusive);
			iter.getTime().setStartTimeInclusive(startTimeInclusive);
			iter.getTime().setEndTimeExclusive(rawLoop.getTime().getEndTimeExclusive());
			iter.getTime().setEndTimeInclusive(rawLoop.getTime().getEndTimeInclusive());
			retLoop.addChild(iter, iter.getTime(), null);
			iter.setDepth(retLoop.getDepth()+1);
		}
		
		return retLoop;
    }
*/
    
    private AbstractTraceNode detectIterations(RawLoopTrace rawLoop) {
    	// First, check if the rawLoop does contain a loop.
    	// Contains a loop = exist at least one child that occurs more than once.
    	
    	OccurrenceRecord[] records = getOccurrenceRecord(rawLoop, 0, rawLoop.getNumOfChildren());
    	// If doesn't contain a loop, convert this rawLoop to functionTrace
    	if (records.length == rawLoop.getNumOfChildren()) {
    		FunctionTrace trace = new FunctionTrace(rawLoop);
    		return trace;
    	}
    	
    	// When a rawloop don't have a cfgNode reference, return null.
    	if (rawLoop.cfgNode == null) {
    		return null;
    	}
    	
    	// If it has, try to identify all iterations using cfgNode.
		IteratedLoopTrace retLoop = new IteratedLoopTrace(rawLoop);

		IterationTrace iter = new IterationTrace(retLoop, retLoop.getNumOfChildren());
		iter.getTime().setStartTimeExclusive(rawLoop.getTime().getStartTimeExclusive());
	    iter.getTime().setStartTimeInclusive(rawLoop.getTime().getStartTimeInclusive());
		
		int lastIndex = -1;
		
		for (int i = 0; i < rawLoop.getNumOfChildren(); i++) {
			int curIndex = rawLoop.cfgNode.getChildIndex(rawLoop.getChildCFGNode(i));
			if (curIndex == -1) { 
				/* When we are unable to locate a child within the cfgNode, we will omit it.
				 */
				System.err.println("Ignored: unexpected CFG ID " + rawLoop.getChildCFGNode(i) + " in loop 0x" + Long.toHexString(rawLoop.cfgNode.vma));
				continue;
			}
			
			// forward flow, simply proceed and add the current child to current iteration
			if (curIndex > lastIndex)
				iter.addChild(rawLoop.getChild(i), rawLoop.getChildTime(i), rawLoop.getChildCFGNode(i));
			// backward flow means end of iteration, end the current iteration and generates a new one.
			else {
				long startTimeExclusive = rawLoop.getChildTime(i).getStartTimeExclusive();
				long startTimeInclusive = rawLoop.getChildTime(i).getStartTimeInclusive();
				
				// end time for the current iteration
				iter.getTime().setEndTimeExclusive(startTimeInclusive);
				iter.getTime().setEndTimeInclusive(startTimeExclusive);

				retLoop.addChild(iter, iter.getTime(), null);
				iter.setDepth(retLoop.getDepth()+1);
				
				// new iteration
				iter = new IterationTrace(retLoop, retLoop.getNumOfChildren());
				iter.getTime().setStartTimeExclusive(startTimeExclusive);
				iter.getTime().setStartTimeInclusive(startTimeInclusive);
				
				iter.addChild(rawLoop.getChild(i), rawLoop.getChildTime(i), rawLoop.getChildCFGNode(i));
			}
		
			lastIndex = curIndex;
		}
		
		if (iter.getNumOfChildren() > 0) {
			iter.getTime().setEndTimeExclusive(rawLoop.getTime().getEndTimeExclusive());
			iter.getTime().setEndTimeInclusive(rawLoop.getTime().getEndTimeInclusive());
			retLoop.addChild(iter, iter.getTime(), null);
			iter.setDepth(retLoop.getDepth()+1);
		}
		
		return retLoop;
    }
    
    /**
     * Input: trace tree containing only FunctionTrace and RawLoop nodes.
     * Output: trace tree containing ProfileNode, FunctionTrace, and IteratedLoop nodes.
     * @param node the input node, which can be a FunctionTrace or RawLoop node.
     * @return the updated node, which can be a ProfileNode, FunctionTrace, or IteratedLoop node.
     */
    // tested and generated expecting output
    public AbstractTreeNode detectLoop(AbstractTreeNode node) {
    	if (node.getName().length()>=4 && node.getName().substring(0, 4).equals("PMPI"))
    		node.clearChildren();
    	
    	if (!(node instanceof AbstractTraceNode)) return null;

    	AbstractTraceNode trace = (AbstractTraceNode) node;
   
    	// For loops, try to divide them into iterations. If can't, turn them into profile nodes.
		if (trace instanceof RawLoopTrace) {
			AbstractTraceNode newTrace = detectIterations((RawLoopTrace)trace);
			if (newTrace != null)  {
				for (int i = 0; i < newTrace.getNumOfChildren(); i++) {
					AbstractTreeNode iter = detectLoop(newTrace.getChild(i));
	    			if (iter != null) newTrace.updateChild(i, iter);
	    		}
				return newTrace;
			}
			else
				return ProfileNode.toProfile(trace);
		}
		
		// For functions, make sure there are no multiple occurrence of the same node (which implies a loop)

    	// Get occurrence records ranked by their first occurred child index.
    	OccurrenceRecord[] records = getOccurrenceRecord(trace, 0, trace.getNumOfChildren());
    	
    	// No loop exists
    	if (records.length == trace.getNumOfChildren()) {
    		for (int i = 0; i < trace.getNumOfChildren(); i++) {
    			AbstractTreeNode child = detectLoop(trace.getChild(i));
    			if (child != null) trace.updateChild(i, child);
    		}
    		return null;
    	}

//TODO while adjusting loops in the code below, some calls with be migrated into certain loops 
   // and lose the CFG connection with its parent. 
    		
    	/*
    	 * A loop exists = at least one node occurred more than once 
    	 * = number of occurrence record is smaller than the number of children.
    	 * 
    	 * If so, try locating all loops.
    	 */

    	Vector<AbstractTreeNode> newChildren = new Vector<AbstractTreeNode>();
    	Vector<TraceTimeStruct> newChildrenTime = new Vector<TraceTimeStruct>();
    	Vector<CFGNode> newChildrenCFGNode = new Vector<CFGNode>();
    	
    	int k = 0;
    	while (k < records.length) {
    		// For child that occurred once and not overlapped with a loop, 
    		// they will remain the child of the current node.
    		while (k < records.length && records[k].occurrence == 1) {
    			newChildren.add(trace.getChild(records[k].firstOccur));
    			newChildrenTime.add(trace.getChildTime(records[k].firstOccur));
    			newChildrenCFGNode.add(trace.getChildCFGNode(records[k].firstOccur));
				k++;
    		}
    		
    		if (k == records.length) break;
    		
    		// If not, detect the loop region and put all overlapped children 
    		// under the new loop node.
    		
    		int firstIdx = k;
    		int firstChild = records[k].firstOccur;
    		int lastChild = records[k].lastOccur;
    		k++;
    		int numRepCat = 1;
    		while (k < records.length && records[k].firstOccur < lastChild) {
    			if (records[k].lastOccur > lastChild) lastChild = records[k].lastOccur;
    			if (records[k].occurrence > 1) numRepCat++;
    			k++;
    		}
    		
    		RawLoopTrace loop = null;
    		
    		// See if the detected loop only consists of repetitions of loops with same ID.
    		// If so, will merge those loops together under one loop node
    		if ((numRepCat == 1) && (trace.getChild(records[firstIdx].firstOccur) instanceof RawLoopTrace)) {
    			RawLoopTrace child = (RawLoopTrace)trace.getChild(records[firstIdx].firstOccur);
    			loop = new RawLoopTrace(child.getID(), child.getName(), child.getDepth(), child.cfgNode);
    		}
    		// If not, allocate a new loop node
    		else {
    	    	if (trace.getDuration() <= detectionCutoff) return ProfileNode.toProfile(trace);
    			//TODO assigning an appropriate loop ID
    			loop = new RawLoopTrace(--detectedLoopID, "LOOP", trace.getDepth()+1, null);
    		}
    		
    		loop.getTime().setStartTimeInclusive(trace.getChildTime(firstChild).getStartTimeInclusive());
    		loop.getTime().setStartTimeExclusive(trace.getChildTime(firstChild).getStartTimeExclusive());
    		loop.getTime().setEndTimeInclusive(trace.getChildTime(lastChild).getEndTimeInclusive());
    		loop.getTime().setEndTimeExclusive(trace.getChildTime(lastChild).getEndTimeExclusive());
    		for (int i = firstChild; i <= lastChild; i++)
    			// This branch will always be taken when a new loop is allocated.
    			if (trace.getChild(i).getID() != loop.getID())
    				loop.addChild(trace.getChild(i), trace.getChildTime(i), trace.getChildCFGNode(i));
    			// This branch will only be taken when the existing loops should be merged.
    		    // Merge the repetitions of the same loop under a single loop node
    			else {
    				RawLoopTrace oldLoop = (RawLoopTrace)trace.getChild(i);
    				for (int j = 0; j < oldLoop.getNumOfChildren(); j++)
    					loop.addChild(oldLoop.getChild(j), oldLoop.getChildTime(j), oldLoop.getChildCFGNode(j));
    			}
    			
    		
    		loop.setDepth(loop.getDepth());
    		newChildren.add(loop);
    		newChildrenTime.add(loop.getTime());
    		newChildrenCFGNode.add(loop.cfgNode);
    	}
    	
    	trace.clearChildren();
    	for (int i = 0; i < newChildren.size(); i++) {
    		AbstractTreeNode child = detectLoop(newChildren.get(i));
    		if (child == null) child = newChildren.get(i);
    		trace.addChild(child, newChildrenTime.get(i), newChildrenCFGNode.get(i));
    	}
    	return null;
    }
}

class OccurrenceRecord {
	final int ID;
	final int firstOccur;
	int lastOccur;
	
	int occurrence = 0;
	long totalDuration = 0;
	long minDuration = Long.MAX_VALUE;
	
	HashSet<Integer> pred = new HashSet<Integer>();
	HashSet<Integer> post = new HashSet<Integer>();
	
	public OccurrenceRecord(int ID, int firstOccur) {
		this.ID = ID;
		this.firstOccur = firstOccur;
	}
}
