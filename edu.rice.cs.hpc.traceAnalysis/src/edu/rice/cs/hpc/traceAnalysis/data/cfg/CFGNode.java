package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public abstract class CFGNode {
	public final long vma;
	
	public CFGNode(long vma) {
		this.vma = vma;
	}
	
	public String toString() {
		return "0x" + Long.toHexString(vma);
	}
}
