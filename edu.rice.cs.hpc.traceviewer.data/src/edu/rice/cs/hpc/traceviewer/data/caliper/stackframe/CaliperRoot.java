package edu.rice.cs.hpc.traceviewer.data.caliper.stackframe;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;

public class CaliperRoot extends CaliperStackFrame {

	@Override
	public String getName() {
		return CaliperUtils.CALIPER_ROOT;
	}
}
