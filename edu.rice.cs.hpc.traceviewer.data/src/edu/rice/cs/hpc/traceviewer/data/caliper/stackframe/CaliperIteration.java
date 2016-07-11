package edu.rice.cs.hpc.traceviewer.data.caliper.stackframe;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;

public class CaliperIteration extends CaliperStackFrame {
	private final CaliperLoop loop;
	private final long iteration;
	
	public CaliperIteration(long iteration, CaliperLoop loop) {
		this.loop = loop;
		this.iteration = iteration;
	}
	
	public CaliperLoop getLoop() {
		return loop;
	}
	
	public long getIterationNumber() {
		return iteration;
	}

	@Override
	public String getDisplayName() {
		return CaliperUtils.ITERATION_PREFIX + iteration + CaliperUtils.ITERATION_AT + loop.getDisplayName();
	}

	@Override
	public String getColorName() {
		return loop.getColorName();
	}
}
