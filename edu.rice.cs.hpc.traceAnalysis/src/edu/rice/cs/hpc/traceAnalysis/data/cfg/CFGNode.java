package edu.rice.cs.hpc.traceAnalysis.data.cfg;

import java.io.Serializable;

public abstract class CFGNode implements Serializable{
	private static final long serialVersionUID = 1083631193415176371L;
	
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
