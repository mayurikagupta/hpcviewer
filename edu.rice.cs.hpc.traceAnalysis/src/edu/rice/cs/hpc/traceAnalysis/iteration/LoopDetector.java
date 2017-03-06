package edu.rice.cs.hpc.traceAnalysis.iteration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.AbstractTraceTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.AbstractLoop;
import edu.rice.cs.hpc.traceAnalysis.data.Iteration;
import edu.rice.cs.hpc.traceAnalysis.data.RawLoopWithIteration;
import edu.rice.cs.hpc.traceAnalysis.data.RawLoopWithoutIteration;
import edu.rice.cs.hpc.traceAnalysis.data.TraceTree;

public class LoopDetector {

	final public static int detectionCutoffRatio = 200;
	final public static int noiseUpperCutoffRatio = 10;
	final public static int noiseLowerCutoffRatio = 2;
	
	final public int detectionCutoff;
	final public int noiseUpperCutoff;
	final public int noiseLowerCutoff;
	
	final public TraceTree traceTree;
	
	private int detectedLoopID = 0;
	
	public LoopDetector(TraceTree traceTree) {
		this.traceTree = traceTree;
		this.detectionCutoff = this.traceTree.sampleFrequency * detectionCutoffRatio;
		this.noiseUpperCutoff = this.traceTree.sampleFrequency * noiseUpperCutoffRatio;
		this.noiseLowerCutoff = this.traceTree.sampleFrequency * noiseLowerCutoffRatio;
	}
	
    private OccurrenceRecord[] getOccurrenceRecord(AbstractTraceTreeNode node, int startChild/*inclusive*/, int endChild/*exclusive*/) {
    	// ID -> Occurrence Record
        HashMap<Integer, OccurrenceRecord> map = new HashMap<Integer, OccurrenceRecord>();
        
        Vector<OccurrenceRecord> records = new Vector<OccurrenceRecord>();
        int predID = -1;
        for (int k = startChild; k < endChild; k++) {
        	int curID = node.getChild(k).getID();
        	if (!map.containsKey(curID)) {
        		OccurrenceRecord record = new OccurrenceRecord(curID, k);
        		if (node.getChild(k) instanceof AbstractLoop)
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
    
    /**
     * Derive a frontier, which consists of a set of CCT nodes that indicates the start or end of an iteration in the loop.
     * To filter the noise introduced by sampling, every instance of the CCT node in the frontier must has a number of samples of more than an adaptively adjusted threshold.
     * @param rootID ID of the CCT node that indicates the start or end of the loop.
     * @param map a map that maps all CCT IDs in the loop to their corresponding occurrence record.
     * @param isBeginFrontier TRUE if deriving a begin frontier, FALSE if deriving an end frontier.
     * @return
     */
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
    
    private boolean skipNode(AbstractTraceTreeNode node) {
    	if (node instanceof Iteration) return false;
    	
	    if (node.getName().length() >= 4 && node.getName().substring(0, 4).equals("PMPI")) return true;
	    if (node.getDuration() < detectionCutoff) return true;
	    
	    return false;
    }
    
    private RawLoopWithIteration detectIterations(RawLoopWithoutIteration loop)  {
    	// Get occurrence records ranked by their first occurred child index.
    	OccurrenceRecord[] records = getOccurrenceRecord(loop, 0, loop.getNumOfChildren());
    	if (records.length == loop.getNumOfChildren()) return null;
    	
		// Try to identify iteration boundaries.
		// TODO we assume no if-statement in the loop at this moment.

		HashMap<Integer, OccurrenceRecord> map = new HashMap<Integer, OccurrenceRecord>();
		for (int i = 0; i < records.length; i++) 
			map.put(records[i].ID, records[i]);
		
		// Derive a begin frontier, which consists of a set of CCT nodes that indicates the start of an iteration.
		// Every instance of the CCT node in the frontier has a duration of more than an adaptively adjusted threshold.
		int startIndex = 0;
		while (map.get(loop.getChild(startIndex).getID()).occurrence == 1) startIndex++;
		HashSet<Integer> beginFrontier = getFrontier(loop.getChild(startIndex).getID(), map, true);
		int beginNoiseCutoff = beginFrontier.size() > 0 ? Integer.MAX_VALUE : 0;
		for (int cctID: beginFrontier)
			if (map.get(cctID).minDuration < beginNoiseCutoff)
				beginNoiseCutoff = (int) map.get(cctID).minDuration;
		
		int endIndex = loop.getNumOfChildren()-1;
		while (map.get(loop.getChild(endIndex).getID()).occurrence == 1) endIndex--;
		HashSet<Integer> endFrontier = getFrontier(loop.getChild(endIndex).getID(), map, false);
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
		
		RawLoopWithIteration retLoop = new RawLoopWithIteration(loop);
		
		//TODO assign appropriate iteration ID
		Iteration iter = new Iteration(retLoop, retLoop.getNumOfChildren());
		long startTimeExclusive = loop.getStartTimeExclusive();
	    long startTimeInclusive = loop.getStartTimeInclusive();
		
		//TODO we assume that two neighboring frontier occurrences are separated by other functions.
		boolean inFrontier = false;
		for (int i = 0; i < loop.getNumOfChildren(); i++) {
			if (!isBeginFrontier) // If end frontier, add the current child before terminating this iteration
				iter.addChild(loop.getChild(i));
			
			// determine if the iteration should be terminated.
			if (frontier.contains(loop.getChild(i).getID())) {
				if (!inFrontier) 
					if (iter.getNumOfChildren() > 0) {
						retLoop.addChild(iter);
						iter.setDepth(loop.getDepth()+1);
						// start time for the current iteration
						iter.setStartTimeExclusive(startTimeExclusive);
						iter.setStartTimeInclusive(startTimeInclusive);
						// start time for the next iteration
						if (isBeginFrontier) {
							startTimeExclusive = loop.getChild(i).getStartTimeExclusive();
							startTimeInclusive = loop.getChild(i).getStartTimeInclusive();
						}
						else {
							startTimeExclusive = loop.getChild(i).getEndTimeInclusive();
							startTimeInclusive = loop.getChild(i).getEndTimeExclusive();
						}
						// end time for the current iteration
						iter.setEndTimeExclusive(startTimeInclusive);
						iter.setEndTimeInclusive(startTimeExclusive);

						iter = new Iteration(retLoop, retLoop.getNumOfChildren());
					}
				inFrontier = true;
			}
			else {
				inFrontier = false;
			}
			
			if (isBeginFrontier) // if begin frontier, add the current child after terminating the previous iteration
				iter.addChild(loop.getChild(i));
		}
		
		if (iter.getNumOfChildren() > 0) {
			retLoop.addChild(iter);
			iter.setDepth(loop.getDepth()+1);
			iter.setStartTimeExclusive(startTimeExclusive);
			iter.setStartTimeInclusive(startTimeInclusive);
			iter.setEndTimeExclusive(loop.getEndTimeExclusive());
			iter.setEndTimeInclusive(loop.getEndTimeInclusive());
		}
		
		return retLoop;
    }
    
    public RawLoopWithIteration detectLoop(AbstractTraceTreeNode node) {
    	if (skipNode(node)) return null;
    	
		if (node instanceof RawLoopWithoutIteration) {
			RawLoopWithIteration loop = detectIterations((RawLoopWithoutIteration)node);
			if (loop != null)
				for (int i = 0; i < loop.getNumOfChildren(); i++)
					detectLoop(loop.getChild(i)); // must return null as all children are objects of class Iteration
			return loop;
		}

    	// Get occurrence records ranked by their first occurred child index.
    	OccurrenceRecord[] records = getOccurrenceRecord(node, 0, node.getNumOfChildren());
    	
    	// No loop exists
    	if (records.length == node.getNumOfChildren()) {
    		for (int i = 0; i < records.length; i++) {
    			RawLoopWithIteration loop = detectLoop(node.getChild(i));
    			if (loop != null)
    				node.replaceChild(loop, i);
    		}
    		return null;
    	}
    	
    	/**
    	 * A loop exists = at least one node occurred more than once 
    	 * = number of occurrence record is smaller than the number of children.
    	 * 
    	 * If so, try locating all loops.
    	 */

    	Vector<AbstractTraceTreeNode> newChildren = new Vector<AbstractTraceTreeNode>();
    	
    	int k = 0;
    	while (k < records.length) {
    		// For every child that occurred once and not overlapped with a loop, 
    		// they will remain the child of the current node.
    		while (k < records.length && records[k].occurrence == 1) {
    			newChildren.add(node.getChild(records[k].firstOccur));
				k++;
    		}
    		
    		if (k == records.length) break;
    		
    		// If not, detect the loop region and put all overlapped children 
    		// under the new loop node.
    		
    		int firstChild = records[k].firstOccur;
    		int lastChild = records[k].lastOccur;
    		k++;
    		while (k < records.length && records[k].firstOccur < lastChild) {
    			if (records[k].lastOccur > lastChild) {
    				lastChild = records[k].lastOccur;
    			}
    			k++;
    		}
    		
    		//TODO assigning an appropriate loop ID
    		RawLoopWithoutIteration loop = new RawLoopWithoutIteration(--detectedLoopID, "LOOP", node.getDepth()+1);
    		loop.setStartTimeInclusive(node.getChild(firstChild).getStartTimeInclusive());
    		loop.setStartTimeExclusive(node.getChild(firstChild).getStartTimeExclusive());
    		loop.setEndTimeInclusive(node.getChild(lastChild).getEndTimeInclusive());
    		loop.setEndTimeExclusive(node.getChild(lastChild).getEndTimeExclusive());
    		for (int i = firstChild; i <= lastChild; i++)
    			loop.addChild(node.getChild(i));
    		loop.setDepth(node.getDepth()+1);
    		newChildren.add(loop);
    	}
    	
    	node.clearChildren();
    	for (AbstractTraceTreeNode child : newChildren) {
    		RawLoopWithIteration loop = detectLoop(child);
    		if (loop != null) child = loop;
    		node.addChild(child);
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
