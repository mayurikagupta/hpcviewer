package edu.rice.cs.hpc.traceviewer.data.caliper.stackframe;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;

public class CaliperPhase extends CaliperStackFrame {
	private final String phase_name;
	
	public CaliperPhase(String phase_name) {
		this.phase_name = phase_name;
	}
	
	public String getPhaseName() {
		return phase_name;
	}

	@Override
	public String getColorName() {
		return CaliperUtils.PHASE_PREFIX + phase_name;
	}

	@Override
	public String getDisplayName() {
		return CaliperUtils.PHASE_PREFIX + phase_name;
	}
}
