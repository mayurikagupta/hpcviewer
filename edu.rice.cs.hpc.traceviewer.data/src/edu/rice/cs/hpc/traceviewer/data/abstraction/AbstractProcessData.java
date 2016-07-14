package edu.rice.cs.hpc.traceviewer.data.abstraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;

/**
 * Provides the data of a given rank
 * 
 * @log
 * - 2016.7 (by Lai Wei) Added this abstraction layer so that hpctraceviewer can display data from multiple sources.
 */
public abstract class AbstractProcessData {
	/** This process's line number & proc id. */
	protected final int lineNum;
	
	public AbstractProcessData(int lineNum) {
		this.lineNum = lineNum;
	}
	
	/** Read in the data */
	public abstract void readInData() throws IOException;
	
	/** Gets the time that corresponds to the index sample. */
	public abstract long getTime(int sample);
	
	/** Returns the AbstractStack corresponding to the sample given */
	public abstract AbstractStack getStack(int sample);
	
	/** Shift the time of all samples by a certain amount */
	public abstract void shiftTimeBy(long lowestStartingTime);
	
	/** Returns the number of samples in this process data*/
	public abstract int size();
	
	/** Returns this process's line number. */
	public int line() {
		return this.lineNum;
	}
	
	/** Returns if data is empty*/
	public abstract boolean isEmpty();
	
	/**
	 * Finds the sample to which the given time most closely corresponds in the data.
	 * @param time the given time
	 * @param usingMidpoint if true, returns the sample whichever is closest; if false, returns the sample whichever is on the left side.
	 * @return the sample index
	 */
	public abstract int findClosestSample(long time, boolean usingMidpoint);
	
	/**
	 * Returns names that occurs frequently at the given depth. Used to create the legend.
	 */
	public ArrayList<String> getFrequentNames(int depth) {
		Hashtable<String, Integer> table = new Hashtable<String, Integer>();
		for (int i = 0; i < this.size(); i++) {
			String name = this.getStack(i).getColorNameAt(depth);
			int oldVal = table.containsKey(name) ? table.get(name) : 0;
			table.put(name, oldVal + 1);
		}
		
		//Transfer as List and sort it
		ArrayList<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(table.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>(){
        	public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
        		return o2.getValue().compareTo(o1.getValue()); // descending order
        	}});
		
		ArrayList<String> names = new ArrayList<String>();
		int k = 0;
		while (k < list.size() && list.get(k).getValue() > this.size() * 0.01) {
			names.add(list.get(k).getKey());
			k++;
		}
		
		return names;
	}
}
