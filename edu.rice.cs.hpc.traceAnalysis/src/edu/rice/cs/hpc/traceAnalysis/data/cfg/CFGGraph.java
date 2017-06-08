package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public abstract class CFGGraph extends CFGNode {
	public final String label;
	public final CFGNode[] nodes;
	public final int[][] edges;
	
	public CFGGraph(long addr, String label, CFGNode[] nodes, int[][] edges) {
		super(addr);
		this.label = label;
		this.nodes = nodes;
		this.edges = edges;
	}
	
	public String toString() {
		String str = super.toString() + ":";
		for (CFGNode n : nodes)
			str += " -> " + n.toString();
		return str;
	}
}
