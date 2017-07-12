package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public class CFGCall extends CFGNode {
	public CFGCall(long vma) {
		super(vma);
	}
	
	public String toString() {
		return "call_" + super.toString();
	}
}
