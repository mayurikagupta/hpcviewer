package edu.rice.cs.hpc.traceAnalysis.data;

public class RawLoopWithIteration extends AbstractRawLoop {
	public RawLoopWithIteration(RawLoopWithoutIteration rawLoop) {
		super(rawLoop.getID(), rawLoop.getName(), rawLoop.getDepth());
		this.startTimeExclusive = rawLoop.startTimeExclusive;
		this.startTimeInclusive = rawLoop.startTimeInclusive;
		this.endTimeExclusive = rawLoop.endTimeExclusive;
		this.endTimeInclusive = rawLoop.endTimeInclusive;
	}

	public String print(int maxDepth) {
		return "+" + super.print(maxDepth+1);
	}
}
