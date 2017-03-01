package edu.rice.cs.hpc.traceAnalysis.iteration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.TraceTreeNode;

public class LoopDetector {

	private static int detectionCutoff = 200;

	private static int noiseUpperCutoff = 10;
	private static int noiseLowerCutoff = 2;
	
    private int getNearestFirstChild(TraceTreeNode node, long sample) {
    	int h = 0; // can be inclusive
    	int t = node.getNumOfChildren(); // must be exclusive
    	if (t != 0 && node.getChild(t-1).getEndSampleInclusive() < sample) return t; // make sure t is exclusive
    	
    	while (h < t - 1) {
    		int mid = (h+t)/2-1;
    		if (node.getChild(mid).getEndSampleInclusive() < sample) h = mid+1;
    		else t = mid+1;
    	}
    	
    	return h;
    }
	
    private OccurrenceRecord[] getOccurrenceRecord(TraceTreeNode node, int startChild/*inclusive*/, int endChild/*exclusive*/) {
    	// CCT Index -> Occurrence Record
        HashMap<Integer, OccurrenceRecord> map = new HashMap<Integer, OccurrenceRecord>();
        
        Vector<OccurrenceRecord> records = new Vector<OccurrenceRecord>();
        int predIndex = -1;
        for (int k = startChild; k < endChild; k++) {
        	int cctIndex = node.getChild(k).getScope().getCCTIndex();
        	if (!map.containsKey(cctIndex)) {
        		OccurrenceRecord record = new OccurrenceRecord(cctIndex, k);
        		records.add(record);
        		map.put(cctIndex, record);
        	}
        	OccurrenceRecord record = map.get(cctIndex);
        	record.lastOccur = k;
        	record.occurrence++;
        	record.totalSamples += node.getChild(k).getNumSamples();
        	record.minSamples = Math.min(record.minSamples, node.getChild(k).getNumSamples());
        	if (predIndex != -1) {
        		record.pred.add(predIndex);
        		map.get(predIndex).post.add(cctIndex);
        	}
        	predIndex = cctIndex;
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
    			if (map.containsKey(cctID) && map.get(cctID).minSamples <= noiseCufoff) 
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
    			if (map.get(cctID).minSamples > noiseCufoff)
    				newFrontier.add(cctID);
    		}
    		
    		if (newFrontier.size() > 0) {
    			frontier.clear();
    			frontier.addAll(newFrontier);
    			noiseCufoff = Integer.MAX_VALUE;
    			for (int cctID: frontier)
    				if (map.get(cctID).minSamples < noiseCufoff)
    					noiseCufoff = (int) map.get(cctID).minSamples;
    		}
    		else break;
		}
		
		if (noiseCufoff == noiseLowerCutoff) frontier.clear();
    	return frontier;
    }
    
    public void detectLoop(TraceTreeNode node, long startSample/*inclusive*/, long endSample/*exclusive*/) {

if (startSample <= node.getStartSampleInclusive() && endSample > node.getEndSampleInclusive())
System.out.print(node.print(node.getDepth()));

    	// Locate the starting/ending child node between startSample and endSample
    	int startChild = getNearestFirstChild(node, startSample);
    	int endChild = getNearestFirstChild(node, endSample);
    	
    	// Get occurrence records ranked by their first occurred child index.
    	OccurrenceRecord[] records = getOccurrenceRecord(node, startChild, endChild);
  	
    	int k = 0;
    	while (k < records.length) {
    		// For every child that occurred once (in a non-loop region), try to detect loops in it.
    		while (k < records.length && records[k].occurrence == 1) {
    			int child = records[k].firstOccur;
				if (!node.getChild(child).getScope().getName().substring(0, 4).equals("PMPI"))
					if (node.getChild(child).getNumSamples() >= detectionCutoff)
						detectLoop(node.getChild(child), node.getChild(child).getStartSampleInclusive(), node.getChild(child).getEndSampleInclusive()+1);
				k++;
    		}
    		
    		if (k == records.length) break;
    		
    		
    		// Try to get the loop region by merging non-colliding repeat regions.
    		int firstIndex = k;
    		int firstChild = records[k].firstOccur;
    		int lastIndex = k;
    		int lastChild = records[k].lastOccur;
    		k++;
    		while (k < records.length && records[k].firstOccur < lastChild) {
    			if (records[k].lastOccur > lastChild) {
    				lastChild = records[k].lastOccur;
    				lastIndex = k;
    			}
    			k++;
    		}
    		
    		if (node.getChild(lastChild).getEndSampleInclusive() - node.getChild(firstChild).getStartSampleInclusive() 
    				< detectionCutoff) continue;
    		
    		// Try to identify iteration boundaries.
    		// TODO we assume no if-statement in the loop at this moment.
    		
    		//if (firstChild - 1 >= startChild) records[firstIndex].pred.remove(node.getChild(firstChild - 1).getScope().getCCTIndex());
    		//if (lastChild + 1 < endChild) records[lastIndex].post.remove(node.getChild(lastChild + 1).getScope().getCCTIndex());

    		HashMap<Integer, OccurrenceRecord> map = new HashMap<Integer, OccurrenceRecord>();
    		for (int i = firstIndex; i < k; i++) 
    			map.put(records[i].cctID, records[i]);
    		
    		// Derive a begin frontier, which consists of a set of CCT nodes that indicates the start of an iteration.
    		// Every instance of the CCT node in the frontier has a duration of more than an adaptively adjusted threshold.
    		HashSet<Integer> beginFrontier = getFrontier(records[firstIndex].cctID, map, true);
			int beginNoiseCutoff = beginFrontier.size() > 0 ? Integer.MAX_VALUE : 0;
			for (int cctID: beginFrontier)
				if (map.get(cctID).minSamples < beginNoiseCutoff)
					beginNoiseCutoff = (int) map.get(cctID).minSamples;
    		
    		HashSet<Integer> endFrontier = getFrontier(records[lastIndex].cctID, map, false);
			int endNoiseCutoff = endFrontier.size() > 0 ? Integer.MAX_VALUE : 0;
			for (int cctID: endFrontier)
				if (map.get(cctID).minSamples < endNoiseCutoff)
					endNoiseCutoff = (int) map.get(cctID).minSamples;
			
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
    		int numIter = 0;
    		boolean inFrontier = false;
    		for (int i = firstChild; i <= lastChild; i++) {
    			if (frontier.contains(node.getChild(i).getScope().getCCTIndex())) {
    				if (!inFrontier) 
    					numIter++;
    				inFrontier = true;
    			}
    			else {
    				inFrontier = false;
    			}
    		}
    		
String print = "  ";
for (int i = 0; i <= node.getDepth(); i++) print += "    ";
print += "-- loop detected from #" + node.getChild(firstChild).getStartSampleInclusive() + " to #" + 
    node.getChild(lastChild).getEndSampleInclusive() + "; # iterations = " + numIter + 
    "; Boundary = " + node.getChild(firstChild).getScope().getName() + 
    "(" + node.getChild(firstChild).getScope().getCCTIndex() + ") ~ "  + node.getChild(lastChild).getScope().getName() + 
    "(" + node.getChild(lastChild).getScope().getCCTIndex() + "), " ;
for (int id : frontier) {
	int child = map.get(id).firstOccur;
	print += node.getChild(child).getScope().getName() + "(" + node.getChild(child).getScope().getCCTIndex() + "), ";
}
print += isBeginFrontier ? beginNoiseCutoff : endNoiseCutoff;
	
System.out.println(print);
    		
			for (int i = firstIndex; i < k; i++) {
    			int child = records[i].firstOccur;
				if (!node.getChild(child).getScope().getName().substring(0, 3).equals("mpi"))
					if (node.getChild(child).getNumSamples() >= detectionCutoff)
						detectLoop(node.getChild(child), node.getChild(child).getStartSampleInclusive(), node.getChild(child).getEndSampleInclusive()+1);
			}
    	}
    }
}

class OccurrenceRecord {
	final int cctID;
	final int firstOccur;
	int lastOccur;
	
	int occurrence = 0;
	long totalSamples = 0;
	long minSamples = Long.MAX_VALUE;
	
	HashSet<Integer> pred = new HashSet<Integer>();
	HashSet<Integer> post = new HashSet<Integer>();
	
	public OccurrenceRecord(int cctID, int firstOccur) {
		this.cctID = cctID;
		this.firstOccur = firstOccur;
	}
}
