package edu.rice.cs.hpc.traceAnalysis.iteration;

import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.TraceTreeNode;

public class Loop {
	TraceTreeNode node;
	
	int loopLevel;
	
	int numIterations;
	Vector<Long> iteractionLocs;
	
	Vector<Loop> subLoops;
}
