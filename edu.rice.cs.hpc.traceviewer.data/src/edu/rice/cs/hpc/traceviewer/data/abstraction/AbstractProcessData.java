package edu.rice.cs.hpc.traceviewer.data.abstraction;

import java.io.IOException;

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
}
