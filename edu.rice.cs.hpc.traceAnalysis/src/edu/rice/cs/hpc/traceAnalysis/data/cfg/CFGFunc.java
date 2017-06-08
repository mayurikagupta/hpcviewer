package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public class CFGFunc extends CFGGraph {
	public CFGFunc(long addr, String label, CFGNode[] nodes, int[][] edges) {
		super(addr, label, nodes, edges);
	}
	
	public String toString() {
		return label + " " + super.toString();
	}
}
