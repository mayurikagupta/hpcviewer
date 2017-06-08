package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public class CFGCall extends CFGNode {
	public CFGCall(long addr) {
		super(addr);
	}
	
	public String toString() {
		return "call_" + super.toString();
	}
}
