package edu.rice.cs.hpc.traceAnalysis.data.tree;

import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

public class TraceTree {
	public final RootTrace root;
	
	public final long begTime;
	public final long endTime;
	
	public final long numSamples;
	public final int sampleFrequency;
	
	public TraceTree(RootTrace root, long begTime, long endTime, long numSamples) {
		this.root = root;
		this.begTime = begTime;
		this.endTime = endTime;
		this.numSamples = numSamples;
		this.sampleFrequency = (int)((endTime - begTime) / (numSamples - 1));
	}
	
	public String print(int maxDepth) {
		return root.print(maxDepth, sampleFrequency * TraceAnalysisUtils.traceCutoffMultiplier);
	}
}
