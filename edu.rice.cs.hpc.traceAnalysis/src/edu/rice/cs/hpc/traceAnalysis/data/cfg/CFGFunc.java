package edu.rice.cs.hpc.traceAnalysis.data.cfg;

import java.util.ArrayList;
import java.util.HashSet;

public class CFGFunc extends CFGGraph {
	public CFGFunc(long vma, String label) {
		super(vma, label);
	}
	
	public CFGFunc(long vma, String label, CFGNode[] nodes, ArrayList<HashSet<CFGNode>> successors) {
		super(vma, label, nodes, successors);
	}
	
	public String toString() {
		return label + " " + super.toString();
	}
}
