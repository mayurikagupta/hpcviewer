package edu.rice.cs.hpc.traceviewer.data.caliper.stackframe;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;

public class CaliperRoot extends CaliperStackFrame {

	@Override
	public String getColorName() {
		return CaliperUtils.CALIPER_ROOT;
	}

	@Override
	public String getDisplayName() {
		return CaliperUtils.CALIPER_ROOT;
	}

}
