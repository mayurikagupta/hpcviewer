package edu.rice.cs.hpc.traceviewer.data.caliper.stackframe;

/**
 * Represent a frame in the caliper stack
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public abstract class CaliperStackFrame {

	/**
	 * Return the name that is used to determine the color of this frame.
	 */
	public abstract String getColorName();
	
	/**
	 * Return the name that is shown to the user.
	 */
	public abstract String getDisplayName();
}
