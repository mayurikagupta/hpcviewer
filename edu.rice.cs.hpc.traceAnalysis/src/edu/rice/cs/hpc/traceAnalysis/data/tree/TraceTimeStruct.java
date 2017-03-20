package edu.rice.cs.hpc.traceAnalysis.data.tree;

public class TraceTimeStruct {
	long startTimeExclusive;
	long startTimeInclusive;
	long endTimeInclusive;
	long endTimeExclusive;

	public TraceTimeStruct() {
		startTimeExclusive = 0;
		startTimeInclusive = 0;
		endTimeInclusive = 0;
		endTimeExclusive = 0;
	}
	
	public TraceTimeStruct(TraceTimeStruct other) {
		startTimeExclusive = other.startTimeExclusive;
		startTimeInclusive = other.startTimeInclusive;
		endTimeInclusive = other.endTimeInclusive;
		endTimeExclusive = other.endTimeExclusive;
	}
	
	public long getStartTimeExclusive() {
		return startTimeExclusive;
	}

	public void setStartTimeExclusive(long startTimeExclusive) {
		this.startTimeExclusive = startTimeExclusive;
	}

	public long getStartTimeInclusive() {
		return startTimeInclusive;
	}

	public void setStartTimeInclusive(long startTimeInclusive) {
		this.startTimeInclusive = startTimeInclusive;
	}

	public long getEndTimeInclusive() {
		return endTimeInclusive;
	}

	public void setEndTimeInclusive(long endTimeInclusive) {
		this.endTimeInclusive = endTimeInclusive;
	}

	public long getEndTimeExclusive() {
		return endTimeExclusive;
	}

	public void setEndTimeExclusive(long endTimeExclusive) {
		this.endTimeExclusive = endTimeExclusive;
	}
	
	public TraceTimeStruct duplicate() {
		return new TraceTimeStruct(this);
	}
}
