package edu.rice.cs.hpc.traceAnalysis.data;

public class TraceTree {
	public final RootNode root;
	
	public final long begTime;
	public final long endTime;
	
	public final long numSamples;
	public final int sampleFrequency;
	
	public TraceTree(RootNode root, long begTime, long endTime, long numSamples) {
		this.root = root;
		this.begTime = begTime;
		this.endTime = endTime;
		this.numSamples = numSamples;
		this.sampleFrequency = (int)((endTime - begTime) / (numSamples - 1));
	}
	
	public String print(int maxDepth) {
		return root.print(maxDepth);
	}
}
