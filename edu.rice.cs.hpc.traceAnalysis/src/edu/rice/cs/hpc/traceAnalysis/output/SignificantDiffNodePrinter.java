package edu.rice.cs.hpc.traceAnalysis.output;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import edu.rice.cs.hpc.traceAnalysis.data.tree.*;
import edu.rice.cs.hpc.traceAnalysis.data.tree.Cluster.ClusterMemberID;
import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

public class SignificantDiffNodePrinter {
	
	static private final double inclusiveHotpathRatio = 0.4;
	static private final double inclusiveMinimunRatio = 0.025;
	
	static private final double minimumDiffRatio = 1.0 / TraceAnalysisUtils.diffCutoffDivider;
	
	private final PrintStream objPrint;
	private final ClusterSetNode clusterNode;
	private final AbstractTraceNode originNode;
	
	private final double totalDiffRatio;
	
	private double sumDiffRatio = 0;
	private ArrayList<AbstractTreeNode[]> majorCallPaths = new ArrayList<AbstractTreeNode[]>();
	
	private final int numIterations;
	
	// Indicate the displayed name of each iteration/thread.
	private final String[] iterationNames;
	
	// Sum of total time spent in a call path for one thread/iteration.
	// sumTime[callpathIdx][iterationIdx]
	private long[][] sumTime;
	
	// Indicate if there are multiple instances of a call path for a thread/iteration. 
	// hasMultipleInstance[callpathIdx] = true means there are multiple instances for callpathIdx for at least one thread/iteration.
	private boolean[] hasMultipleInstance;
	
	// Time spent in each instance of a call path for one thread/iteration.
	// perInstanceTime[callpathIdx][iterationIdx][instanceIdx]
	private long[][][] perInstanceTime;
	// instanceID[callpathIdx][instanceIdx] ID of each instance for a call path.
	private ClusterMemberID[][] instanceID;
	
	private SignificantDiffNodePrinter(PrintStream objPrint, ClusterSetNode clusterNode) {
		this.objPrint = objPrint;
		this.clusterNode = (ClusterSetNode) clusterNode.duplicate();
		this.originNode = this.clusterNode.getOrigin();
		
		this.clusterNode.getRep().stretchDiffScore(1, (double)this.clusterNode.getRep().getDuration()
				* (double)this.clusterNode.getRep().getWeight() * (this.clusterNode.getRep().getWeight() - 1));
		this.totalDiffRatio = this.clusterNode.getRep().getMetrics().getInclusiveDiffScore();
		
		this.numIterations = this.originNode.getNumOfChildren();
		
		this.iterationNames = new String[numIterations];
		for (int i = 0; i < numIterations; i++)
			iterationNames[i] = this.originNode.getChild(i).getName();
	}
	
	// TODO The current implementation only works when no loop has ever been turned into profiles along the call path.
	private void extractInstanceID() {
		hasMultipleInstance = new boolean[majorCallPaths.size()];
		instanceID = new ClusterMemberID[majorCallPaths.size()][];
		
		// For each callpath
		for (int k = 0; k < majorCallPaths.size(); k++) {
			AbstractTreeNode[] callpath = majorCallPaths.get(k);
			hasMultipleInstance[k] = false;
			
			// find the deepest ClusterNode in the callpath
			for (int i = callpath.length-1; i >= 0; i--)
				if (callpath[i] instanceof ClusterSetNode) {
					ClusterSetNode node = (ClusterSetNode) callpath[i];
					HashSet<ClusterMemberID> instanceNameSet = new HashSet<ClusterMemberID>();
					for (int j = 0; j < node.getNumOfClusters(); j++) 
						for (ClusterMemberID member : node.getCluster(j).getMembers()) {
							ClusterMemberID dup = member.duplicate();
							dup.remove();
							instanceNameSet.add(dup);
						}
					
					if (instanceNameSet.size() > 0) {
						hasMultipleInstance[k] = true;
						instanceID[k] = new ClusterMemberID[instanceNameSet.size()];
						instanceID[k] = instanceNameSet.toArray(instanceID[k]);
						Arrays.sort(instanceID[k]);
						
						//objPrint.print("instanceNameSet for " + node.printLargeDiffNodes(node.getDepth(), 0, null, 1));
						//for (ClusterMemberID member : instanceID[k])
						//	objPrint.print(member.toString() + ", ");
						//objPrint.println();
						
					}
					
					break;
				}
		}
	}
	
	private void extractTime(AbstractTreeNode node, int callpathIdx, int callpathDepth, int iterationIdx, int instanceIdx, String instanceName) {
		assert(node.getID() == majorCallPaths.get(callpathIdx)[callpathDepth].getID());
		
		if (node.getID() != majorCallPaths.get(callpathIdx)[callpathDepth].getID())
			objPrint.println("************ERROR******************");
		
		if (callpathDepth == majorCallPaths.get(callpathIdx).length - 1) {
			long time = (node.getMinDuration() + node.getMaxDuration()) / 2;
			if (instanceName.length() > 0)
				perInstanceTime[callpathIdx][iterationIdx][instanceIdx] = time;
			sumTime[callpathIdx][iterationIdx] += time;
			return;
		}
		
		if (majorCallPaths.get(callpathIdx)[callpathDepth] instanceof ProfileNode) {
			ProfileNode profile = (node instanceof ProfileNode) ? (ProfileNode)node : ProfileNode.toProfile(node);
			for (ProfileNode child : profile.getChildMap().values())
				if (child.getID() == majorCallPaths.get(callpathIdx)[callpathDepth+1].getID()) {
					extractTime(child, callpathIdx, callpathDepth+1, iterationIdx, instanceIdx, instanceName);
					return;
				}
		}
		
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++)
				if (trace.getChild(i).getID() == majorCallPaths.get(callpathIdx)[callpathDepth+1].getID()) {
					extractTime(trace.getChild(i), callpathIdx, callpathDepth+1, iterationIdx, instanceIdx, instanceName);
					return;
				}
		}
		
		if (node instanceof ClusterSetNode) {
			AbstractTraceNode origin = ((ClusterSetNode) node).getOrigin();
			for (int i = 0; i < origin.getNumOfChildren(); i++) {
				String newInstanceName = String.valueOf(i);
				if (instanceName.length() != 0) newInstanceName = instanceName + "." + newInstanceName;
				
				while (instanceID[callpathIdx][instanceIdx].toString().length() < newInstanceName.length() 
						|| !instanceID[callpathIdx][instanceIdx].toString().substring(0, newInstanceName.length()).equals(newInstanceName))
					instanceIdx++;
				extractTime(origin.getChild(i), callpathIdx, callpathDepth+1, iterationIdx, instanceIdx, newInstanceName);
			}
		}
	}
	
	private void extractRuntimeTable() {
		extractInstanceID();
		
		sumTime = new long[majorCallPaths.size()+1][numIterations];
		perInstanceTime = new long[majorCallPaths.size()][][];
		
		for (int i = 0; i < numIterations; i++)
			sumTime[majorCallPaths.size()][i] = (originNode.getChild(i).getMinDuration() + 
					originNode.getChild(i).getMaxDuration()) / 2;
		
		for (int k = 0; k < majorCallPaths.size(); k++) {
			if (hasMultipleInstance[k]) 
				perInstanceTime[k] = new long [numIterations][instanceID[k].length];
			
			for (int i = 0; i < numIterations; i++) {
				extractTime(originNode.getChild(i), k, 0, i, 0, "");
				sumTime[majorCallPaths.size()][i] -= sumTime[k][i];
			}
		}
	}
	
	private String printTime(long t) {
		return t / 1000000 + "." + t / 100000 % 10 + t / 10000 % 10 + t / 1000 % 10;
	}
	
	private void printRuntimeTableCSV() {
		
		// Print summary table
		
		for (int i = 0; i < numIterations; i++) objPrint.print("\t" + iterationNames[i]);
		objPrint.println();
		
		for (int k = 0; k < majorCallPaths.size(); k++) {
			objPrint.print("Cpid_" + majorCallPaths.get(k)[majorCallPaths.get(k).length-1].getID());
			
			for (int i = 0; i < numIterations; i++)
				objPrint.print("\t" + printTime(sumTime[k][i]));
			objPrint.println();
		
			
			//for (int i = 0; i < numIterations; i++) {
			//	for (int j = 0; j < instanceID[k].length; j++)
			//		objPrint.print(perInstanceTime[k][i][j] + ", ");
			//	objPrint.println(" = " + sumTime[k][i]);
			//}
			
		}
		
		objPrint.print("Others");
		for (int i = 0; i < numIterations; i++)
			objPrint.print("\t" + printTime(sumTime[majorCallPaths.size()][i]));
		objPrint.println();
		
		// Print perIteration table
		for (int k = 0; k < majorCallPaths.size(); k++)
			if (hasMultipleInstance[k]) {
				objPrint.println();
				objPrint.print("Cpid_" + majorCallPaths.get(k)[majorCallPaths.get(k).length-1].getID());
				
				for (int i = 0; i < instanceID[k].length; i++) objPrint.print("\tI" + instanceID[k][i]);
				objPrint.println();
				
				for (int i = 0; i < numIterations; i++) {
					objPrint.print(iterationNames[i]);
					for (int j = 0; j < instanceID[k].length; j++)
						objPrint.print("\t" + printTime(perInstanceTime[k][i][j]));
					objPrint.println();
				}
			}
	}
	
	private void findSignificantDiffNode() {
		findSignificantDiffNode(clusterNode.getRep(), new ArrayList<AbstractTreeNode>());
		
		// Sort by significance
		for (int i = 0; i < majorCallPaths.size(); i++)
			for (int j = i+1; j < majorCallPaths.size(); j++)
				if (majorCallPaths.get(i)[majorCallPaths.get(i).length-1].getMetrics().getExclusiveDiffScore() < 
						majorCallPaths.get(j)[majorCallPaths.get(j).length-1].getMetrics().getExclusiveDiffScore()) {
					AbstractTreeNode[] swap = majorCallPaths.get(j);
					majorCallPaths.set(j, majorCallPaths.get(i));
					majorCallPaths.set(i, swap);
				}
		
		for (AbstractTreeNode[] callpath : majorCallPaths) {
			for (AbstractTreeNode node: callpath)
				objPrint.print(node.printLargeDiffNodes(node.getDepth(), 0, 1));
		}
		
		extractRuntimeTable();
		printRuntimeTableCSV();
	}
	
	private void findSignificantDiffNode(AbstractTreeNode node, ArrayList<AbstractTreeNode> callpath) {
		if (node.getMetrics().getInclusiveDiffScore() < totalDiffRatio * inclusiveMinimunRatio) return;
		
		// First, determine if child of the node would be better chosen as a significant diff node
		boolean betterChild = false;
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++) {
				if (trace.getChild(i).getMetrics().getInclusiveDiffScore() > node.getMetrics().getExclusiveDiffScore() * inclusiveHotpathRatio) {
					betterChild = true;
					break;
				}
			}
		} else if (node instanceof ProfileNode) {
			for (ProfileNode child : ((ProfileNode) node).getChildMap().values())
				if (child.getMetrics().getInclusiveDiffScore() > node.getMetrics().getExclusiveDiffScore() * inclusiveHotpathRatio) {
					betterChild = true;
					break;
				}
		} else betterChild = true; // always true for ClusterSetNode
		
		if (callpath.size() == 0) betterChild = true;
		
		if (!betterChild) {
			sumDiffRatio += node.getMetrics().getExclusiveDiffScore();
			callpath.add(node);
			majorCallPaths.add(callpath.toArray(new AbstractTreeNode[callpath.size()]));
			callpath.remove(callpath.size()-1);
			return;
		}
		
		// If a significant child is found, iterate through all children
		callpath.add(node);
		
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++)
				findSignificantDiffNode(trace.getChild(i), callpath);
		} else if (node instanceof ProfileNode) {
			for (ProfileNode child : ((ProfileNode) node).getChildMap().values())
				findSignificantDiffNode(child, callpath);
		} else if (node instanceof ClusterSetNode) {
			findSignificantDiffNode(((ClusterSetNode) node).getRep(), callpath);	
		}
		
		callpath.remove(callpath.size()-1);
	}
	
	public static void printAllCluster(PrintStream objPrint, AbstractTreeNode node) {
		if (node instanceof ClusterSetNode) {
			SignificantDiffNodePrinter printer = new SignificantDiffNodePrinter(objPrint, (ClusterSetNode) node);
			if (printer.totalDiffRatio > minimumDiffRatio) {
				objPrint.println("\n@ " + node.getName() + ", totalDiffRatio = " + String.format("%.2f", printer.totalDiffRatio * 100) + "%");
				printer.findSignificantDiffNode();
				objPrint.println("remaining diff ratio = " + String.format("%.2f", (printer.totalDiffRatio - printer.sumDiffRatio) * 100) + "%");
			}
		} else if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++)
				printAllCluster(objPrint, trace.getChild(i));
		}
	}
}
