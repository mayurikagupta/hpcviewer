package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public abstract class CFGNode {
	public final long addr;
	
	public CFGNode(long addr) {
		this.addr = addr;
	}
	
	public String toString() {
		return "0x" + Long.toHexString(addr);
	}
}
