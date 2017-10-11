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
	
	public boolean equals(Object other) {
		if (other instanceof CFGFunc)
			if (this.vma == ((CFGFunc)other).vma)
				return true;
		return false;
	}
	
	public String toString() {
		return "func_" + super.toString();
	}
}
