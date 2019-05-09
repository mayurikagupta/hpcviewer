package edu.rice.cs.hpc.data.util;

public class Constants {

	//-----------------------------------------------------------
	// CONSTANTS
	//-----------------------------------------------------------
	
	static public final int MULTI_PROCESSES = 1;
	static public final int MULTI_THREADING = 2;

	public static final int SIZEOF_LONG  = Long.SIZE / Byte.SIZE;
	public static final int SIZEOF_INT 	 = Integer.SIZE / Byte.SIZE;
	public static final int SIZEOF_FLOAT = Float.SIZE / Byte.SIZE;

	static public final String DATABASE_FILENAME = "experiment.xml";
	
	/*
	https://github.com/HPCToolkit/hpctoolkit/blob/datacentric/src/lib/prof-lean/hpcrun-fmt.h
	
 	#define NODE_TYPE_REGULAR             0
	#define NODE_TYPE_LEAF                1
	#define NODE_TYPE_ALLOCATION          2
	#define NODE_TYPE_GLOBAL_VARIABLE     4
	#define NODE_TYPE_MEMACCESS           8
	#define NODE_TYPE_ROOT               16
	#define NODE_TYPE_UNKNOWN_ATTRIBUTE  32
    */
	static public final int HPCRUN_NODE_TYPE_REGULAR           =  0;
	static public final int HPCRUN_NODE_TYPE_LEAF              =  1;
	static public final int HPCRUN_NODE_TYPE_ALLOCATION        =  2;
	static public final int HPCRUN_NODE_TYPE_GLOBAL_VARIABLE   =  4;
	static public final int HPCRUN_NODE_TYPE_MEMACCESS         =  8;
	static public final int HPCRUN_NODE_TYPE_MEMACCESS_ROOT    = 10;
	static public final int HPCRUN_NODE_TYPE_ROOT              = 16;
	static public final int HPCRUN_NODE_TYPE_UNKNOWN_ATTRIBUTE = 32;
	
	static public enum NODE_TYPE {
		NODE_TYPE_REGULAR, 
		NODE_TYPE_LEAF,
		NODE_TYPE_ALLOCATION,
		NODE_TYPE_GLOBAL_VARIABLE,
		NODE_TYPE_MEMACCESS,
		NODE_TYPE_ROOT,
		NODE_TYPE_MEMACCESS_ROOT,
		NODE_TYPE_UNKNOWN_ATTRIBUTE;
	}
	
	/**
	 * convert from hpcrun node type (based on c) to hpcdata node type (java-style)
	 * 
	 * @param node_type
	 * @return
	 */
	static public NODE_TYPE getNodeTypeFromHpcrun(int node_type) {
		switch (node_type) {
		
		case HPCRUN_NODE_TYPE_REGULAR:
			return NODE_TYPE.NODE_TYPE_REGULAR;
			
		case HPCRUN_NODE_TYPE_LEAF:
			return NODE_TYPE.NODE_TYPE_LEAF;
			
		case HPCRUN_NODE_TYPE_ALLOCATION:
			return NODE_TYPE.NODE_TYPE_ALLOCATION;
			
		case HPCRUN_NODE_TYPE_GLOBAL_VARIABLE:
			return NODE_TYPE.NODE_TYPE_GLOBAL_VARIABLE;
			
		case HPCRUN_NODE_TYPE_MEMACCESS:
			return NODE_TYPE.NODE_TYPE_MEMACCESS;
			
		case HPCRUN_NODE_TYPE_ROOT:
			return NODE_TYPE.NODE_TYPE_ROOT;
			
		case HPCRUN_NODE_TYPE_MEMACCESS_ROOT:
			return NODE_TYPE.NODE_TYPE_MEMACCESS_ROOT;
			
		case HPCRUN_NODE_TYPE_UNKNOWN_ATTRIBUTE:
			return NODE_TYPE.NODE_TYPE_UNKNOWN_ATTRIBUTE;
		}
		
		return NODE_TYPE.NODE_TYPE_REGULAR;
	}
}
