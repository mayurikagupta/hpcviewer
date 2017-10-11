package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public abstract class CFGNode {
	public final long vma;
	
	public CFGNode(long vma) {
		this.vma = vma;
	}
	
	public abstract boolean equals(Object other);
	
	public int hashCode() {
		return Long.valueOf(vma).hashCode();
	}
	
	public String toString() {
		return "0x" + Long.toHexString(vma);
	}
}
