package edu.rice.cs.hpc.traceAnalysis.data.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class CFGGraph extends CFGNode {
	public final boolean valid;
	public final String label;
	public final CFGNode[] nodes;
	public final ArrayList<HashSet<CFGNode>> successors;
	private HashMap<CFGNode, Integer> childIndexMap = null;
	
	public CFGGraph(long vma, String label) {
		super(vma);
		this.valid = false;
		this.label = label;
		this.nodes = null;
		this.successors = null;
	}
	
	public CFGGraph(long vma, String label, CFGNode[] nodes, ArrayList<HashSet<CFGNode>> successors) {
		super(vma);
		this.valid = true;
		this.label = label;
		this.nodes = nodes;
		this.successors = successors;
	}
	
	private void buildChildIndex() {
		childIndexMap = new HashMap<CFGNode, Integer>();
		
		for (int i = 0; i < nodes.length; i++)
			childIndexMap.put(nodes[i], i);
	}
	
	/**
	 * @return 
	 * -1 if node1 is a predecessor of node2;
	 * 0 if node1 and node2 has no predecessor/successor relationship; 
	 * 1 if node2 is a predecessor of node1;
	 */
	public int compareChild(CFGNode node1, CFGNode node2) {
		assert(childIndexMap != null);
		assert(childIndexMap.containsKey(node1));
		assert(childIndexMap.containsKey(node2));
		
		int idx1 = childIndexMap.get(node1);
		int idx2 = childIndexMap.get(node2);
		
		if (successors.get(idx1).contains(node2)) return -1;
		if (successors.get(idx2).contains(node1)) return 1;
		return 0;
	}
	
	public boolean hasChild(CFGNode node) {
		if (childIndexMap == null) buildChildIndex();
		
		return childIndexMap.containsKey(node);
	}
	
	public String toDetailedString() {
		String str = super.toString() + ":";
		if (this.valid)
			for (CFGNode n : nodes)
				str += " -> " + n.toString();
		return str;
	}
}
