package edu.rice.cs.hpc.traceviewer.data.caliper.stackframe;

/**
 * Represent a frame in the caliper stack
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public abstract class CaliperStackFrame {

	/**
	 * Return the name, used to determine the color, of this frame.
	 */
	public abstract String getColorName();
	
	/**
	 * Return the name, used for displaying, of this frame.
	 */
	public abstract String getDisplayName();
}
