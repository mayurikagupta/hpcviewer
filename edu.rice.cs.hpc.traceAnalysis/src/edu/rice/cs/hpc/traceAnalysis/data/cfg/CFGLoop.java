package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public class CFGLoop extends CFGGraph{
	public CFGLoop(long addr, String label, CFGNode[] nodes, int[][] edges) {
		super(addr, label, nodes, edges);
	}
	
	public String toString() {
		return "[loop_" + super.toString() + "]";
	}
}
