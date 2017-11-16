package edu.rice.cs.hpc.traceAnalysis.utils;

import java.util.HashMap;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGFunc;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGLoop;

public class TraceAnalysisUtils {
	static public final int traceCutoffMultiplier = 0;
	static public final int loopAverageIterationLengthCutoffMultiplier = 100;
	
	static public final int diffCutoffDivider = 1000;
	//public static int profileCutoffMultiplier = 10;
	
	static public long computeWeightedAverage(long value1, int weight1, long value2, int weight2) {
		long total = value1 * weight1 + value2 * weight2; // TODO may overflow
		int divisor = weight1 + weight2;
		return (total + divisor / 2) / divisor;
	}
	
	
	
	static private HashMap<Long, CFGFunc> CFGFuncMap = new HashMap<Long, CFGFunc>();
	static private HashMap<Long, CFGLoop> CFGLoopMap = new HashMap<Long, CFGLoop>();
	
	static public CFGFunc lookupCFGFunc(long addr) {
		return CFGFuncMap.get(addr);
	}
	
	static public CFGLoop lookupCFGLoop(long addr) {
		return CFGLoopMap.get(addr);
	}
	
	static public void setCFGFuncMap(HashMap<Long, CFGFunc> map) {
		CFGFuncMap = map;
	}
	
	static public void setCFGLoopMap(HashMap<Long, CFGLoop> map) {
		CFGLoopMap = map;
	}
}
