package edu.rice.cs.hpc.traceAnalysis.data.cfg;

import java.util.ArrayList;
import java.util.HashSet;

public class CFGLoop extends CFGGraph{
	public CFGLoop(long addr, String label) {
		super(addr, label);
	}
	
	public CFGLoop(long addr, String label, CFGNode[] nodes, ArrayList<HashSet<CFGNode>> successors) {
		super(addr, label, nodes, successors);
	}
	
	public String toString() {
		return "[loop_" + super.toString() + "]";
	}
}
