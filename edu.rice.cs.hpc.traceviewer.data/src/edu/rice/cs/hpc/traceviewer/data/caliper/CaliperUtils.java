package edu.rice.cs.hpc.traceviewer.data.caliper;

import java.io.File;

import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperRoot;

/**
 * Global caliper-related variables 
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public class CaliperUtils {
	/**
	 * Input caliper data file related constants
	 */
	public static final String CALIPER_DIR = "caliper" + File.separatorChar;
	public static final String SUMMARY_FILENAME = "summary";
	public static final String RECORD_INDEX_FILE_PREFIX = "index_";
	
	public static final char SECTION_SEPARATOR = ',';
	public static final String FRAME_SEPARATOR = "/";
	
	public static final char PHASE_INDICATOR = 'P';
	public static final char ITERATION_INDICATOR = 'I';
	public static final char ITERATION_SEPARATOR = '@';
	
	public static final int SIZE_INT = Integer.SIZE / Byte.SIZE;
	public static final int SIZE_LONG = Long.SIZE / Byte.SIZE;
	
	public static final int ERROR = -1;
	
	public static final String CALIPER_DATA_ERROR = "Caliper data error: ";
	
	/**
	 * Output related constants
	 */
	public static final String CALIPER_ROOT = "Caliper Root";
	public static final CaliperRoot CALIPER_ROOT_FRAME = new CaliperRoot();
	
	public static final String PHASE_PREFIX = "Phase ";
	public static final String LOOP_PREFIX = "Loop ";
	public static final String ITERATION_PREFIX = "#";
	public static final String ITERATION_AT = " @ ";
}
