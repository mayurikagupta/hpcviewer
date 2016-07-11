package edu.rice.cs.hpc.traceviewer.data.caliper.stackframe;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;

public class CaliperRoot extends CaliperStackFrame {

	@Override
	public String getDisplayName() {
		return getColorName();
	}

	@Override
	public String getColorName() {
		return CaliperUtils.CALIPER_ROOT;
	}
}
