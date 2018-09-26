package edu.rice.cs.hpc.data.experiment.scope;

public class ScopeID {
	private int cctID;
	private int callerID;
	
	public ScopeID(int cctID) {
		this.callerID = 0;
		this.cctID = cctID;
	}
	
	public ScopeID(int callerID, int cctID) {
		this.callerID = callerID;
		this.cctID = cctID;
	}
	
	public int hashCode() {
		return cctID + callerID;
	}
	
	public boolean equals(Object o) {
		if (o instanceof ScopeID) {
			ScopeID other = (ScopeID) o;
			if (cctID == other.cctID && callerID == other.callerID)
				return true;
		}
		return false;
	}
	
	public String toString() {
		if (callerID != 0)
			return "[c:" + callerID + "/" + cctID + "] ";
		else
			return "[c:" + cctID + "] ";
	}
}
