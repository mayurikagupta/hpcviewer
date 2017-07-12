package edu.rice.cs.hpc.traceAnalysis.data.cfg;

import java.util.HashMap;

public abstract class CFGGraph extends CFGNode {
	public final String label;
	public final CFGNode[] nodes;
	public final int[][] edges;
	private HashMap<CFGNode, Integer> childIndexMap = null;
	
	public CFGGraph(long vma, String label, CFGNode[] nodes, int[][] edges) {
		super(vma);
		this.label = label;
		this.nodes = nodes;
		this.edges = edges;
	}
	
	private void buildChildIndex() {
		childIndexMap = new HashMap<CFGNode, Integer>();
		
		for (int i = 0; i < nodes.length; i++)
			childIndexMap.put(nodes[i], i);
	}
	
	public int getChildIndex(CFGNode node) {
		if (childIndexMap == null) buildChildIndex();
		
		if (childIndexMap.containsKey(node)) return childIndexMap.get(node);
		return -1;
	}
	
	public String toString() {
		String str = super.toString() + ":";
		for (CFGNode n : nodes)
			str += " -> " + n.toString();
		return str;
	}
}
