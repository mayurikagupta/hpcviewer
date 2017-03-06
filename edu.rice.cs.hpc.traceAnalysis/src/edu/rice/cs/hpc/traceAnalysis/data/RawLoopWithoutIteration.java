package edu.rice.cs.hpc.traceAnalysis.data;

public class RawLoopWithoutIteration extends AbstractRawLoop {
	public RawLoopWithoutIteration(int ID, String name, int depth) {
		super(ID, name, depth);
	}
	
	public String print(int maxDepth) {
		return "*" + super.print(maxDepth);
	}
}
