package edu.rice.cs.hpc.traceviewer.data.abstraction;

import java.util.Vector;

/**
 * The stack that provide the details of a sample.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Added this abstraction layer so that hpctraceviewer can display data from multiple sources.
 */
public abstract class AbstractStack {
	
	/** Maximun depth of the stack */
	protected final int maxDepth;
	
	public static final String NULL_NAME = "-Outside Data Range-";
	
	public AbstractStack(int maxDepth) {
		this.maxDepth = maxDepth;
	}
	
	/**
	 * Retrieve the name, for coloring purposes, of a stack frame at given depth.
	 */
	public abstract String getColorNameAt(int depth);
	
	/**
	 * Retrieve the names, for displaying purposes, of all stack frames on the stack.
	 */
	public abstract Vector<String> getDisplayNames();
	
	/*******************************
	 * Retrieve the maximum depth of this stack
	 *******************************/
	public int getMaxDepth()
	{
		return maxDepth;
	}
	
}
