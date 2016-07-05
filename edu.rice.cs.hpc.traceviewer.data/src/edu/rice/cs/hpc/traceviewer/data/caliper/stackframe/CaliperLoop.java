package edu.rice.cs.hpc.traceviewer.data.caliper.stackframe;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;

public class CaliperLoop extends CaliperStackFrame {
	private final String loop_name;
	
	public CaliperLoop(String loop_name) {
		this.loop_name = loop_name;
	}
	
	public String getLoopName() {
		return loop_name;
	}

	@Override
	public String getColorName() {
		return CaliperUtils.LOOP_PREFIX + loop_name;
	}

	@Override
	public String getDisplayName() {
		return CaliperUtils.LOOP_PREFIX + loop_name;
	}
}
