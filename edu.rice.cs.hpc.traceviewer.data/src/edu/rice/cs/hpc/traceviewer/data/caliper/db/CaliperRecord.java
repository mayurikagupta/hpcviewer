package edu.rice.cs.hpc.traceviewer.data.caliper.db;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperStack;

/**
 * Represents a piece of caliper record.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public class CaliperRecord {
	public final long timestamp;
	public final CaliperStack stack;
	
	public CaliperRecord(long timestamp, CaliperStack stack) {
		this.timestamp = timestamp;
		this.stack = stack;
	}
}
