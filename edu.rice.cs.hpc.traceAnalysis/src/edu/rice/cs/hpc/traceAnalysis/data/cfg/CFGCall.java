package edu.rice.cs.hpc.traceAnalysis.data.cfg;

public class CFGCall extends CFGNode {
	private static final long serialVersionUID = 2739711733665207359L;

	public CFGCall(long vma) {
		super(vma);
	}
	
	public boolean equals(Object other) {
		if (other instanceof CFGCall)
			if (this.vma == ((CFGCall)other).vma)
				return true;
		return false;
	}
	
	public String toString() {
		return "call_" + super.toString();
	}
}
