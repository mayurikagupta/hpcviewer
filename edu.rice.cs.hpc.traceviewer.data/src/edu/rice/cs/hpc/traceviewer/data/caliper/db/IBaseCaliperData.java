package edu.rice.cs.hpc.traceviewer.data.caliper.db;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.*;

/**
 * The interface that provides info available in a caliper summary.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public interface IBaseCaliperData extends IBaseData {
	/**
	 * Return if the caliper data is valid
	 */
	public boolean isCaliperDataOpen();
	
	/**
	 * Return the directory that contains caliper files.
	 */
	public String getCaliperDir();
	
	/**
	 * Return the proc ID of a given rank ID
	 */
	public int getProcID(int rankID);
	
	/**
	 * Return the thread ID of a given rank ID
	 */
	public int getThreadID(int rankID);
	
	/**
	 * Return the stack frame of caliper root, phases, and loops.
	 */
	public CaliperStackFrame[] getStackFrames();
	
	/**
	 * Get the caliper phase given its name
	 */
	public CaliperPhase getPhase(String phaseName);
	
	/**
	 * Get the caliper loop given its name
	 */
	public CaliperLoop getLoop(String loopName);
}
