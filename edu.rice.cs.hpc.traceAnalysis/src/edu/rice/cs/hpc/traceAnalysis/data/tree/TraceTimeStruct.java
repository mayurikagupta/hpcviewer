package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.io.Serializable;

import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

public class TraceTimeStruct implements Serializable {
	private static final long serialVersionUID = 4846970525263781853L;

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
	
	public void shiftTime(long variable) {
		startTimeExclusive += variable;
		startTimeInclusive += variable;
		endTimeInclusive += variable;
		endTimeExclusive += variable;
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

	static public TraceTimeStruct mergeTimeStruct(TraceTimeStruct time1, int weight1, TraceTimeStruct time2, int weight2) {
		if (time1 == null || time2 == null) return null;
		
		TraceTimeStruct mergedTime = new TraceTimeStruct();
		mergedTime.setEndTimeExclusive(TraceAnalysisUtils.computeWeightedAverage(time1.getEndTimeExclusive(), weight1,
				time2.getEndTimeExclusive(), weight2));
		mergedTime.setEndTimeInclusive(TraceAnalysisUtils.computeWeightedAverage(time1.getEndTimeInclusive(), weight1,
				time2.getEndTimeInclusive(), weight2));
		mergedTime.setStartTimeExclusive(TraceAnalysisUtils.computeWeightedAverage(time1.getStartTimeExclusive(), weight1,
				time2.getStartTimeExclusive(), weight2));
		mergedTime.setStartTimeInclusive(TraceAnalysisUtils.computeWeightedAverage(time1.getStartTimeInclusive(), weight1,
				time2.getStartTimeInclusive(), weight2));
		return mergedTime;
	}
}
