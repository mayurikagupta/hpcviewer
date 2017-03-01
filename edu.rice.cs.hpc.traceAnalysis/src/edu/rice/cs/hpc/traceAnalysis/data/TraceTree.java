package edu.rice.cs.hpc.traceAnalysis.data;

public class TraceTree {
	public final TraceTreeNode root;
	
	public final long begTime;
	public final long endTime;
	
	public final long numSamples;
	
	public TraceTree(TraceTreeNode root, long begTime, long endTime, long numSamples) {
		this.root = root;
		this.begTime = begTime;
		this.endTime = endTime;
		this.numSamples = numSamples;
	}
	
	public String print(int maxDepth) {
		return root.print(maxDepth);
	}
}
