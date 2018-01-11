package edu.rice.cs.hpc.traceAnalysis.output;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGLoop;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.IterationTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterSetNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ProfileNode;

public class PerformanceImprovementEstimator {
	
	static private final double minimumImprovementGroupRatio = 0.02;
	static private final double minimumImprovementItemRatio = 0.003; // 0.005
	static private final double minimumSyncRatio = 0.001; //0.0005; // 0.001;
	
	static private final double hotpathRatio = 0.7;
	static private final double minimunChildRatio = 0.02;
	
	private final PrintStream objPrint;
	private final ClusterSetNode clusterNode;
	
	private final long totalDuration;
	private final int numProc;
	
	private HashMap<AbstractTreeNode, ArrayList<AbstractTreeNode>> syncChildNodesMap;
	private ArrayList<ImprovementGroup> improvementReport;
	
	private Stack<AbstractTreeNode> lastSyncCallpath;
	
	public PerformanceImprovementEstimator(PrintStream objPrint, ClusterSetNode clusterNode) {
		this.objPrint = objPrint;
		this.clusterNode = clusterNode;
		
		this.numProc = this.clusterNode.getRep().getWeight();
		this.totalDuration = this.clusterNode.getDuration();
		
		this.syncChildNodesMap = new HashMap<AbstractTreeNode, ArrayList<AbstractTreeNode>>();
		this.improvementReport = new ArrayList<ImprovementGroup>();
		
		this.lastSyncCallpath = null;
		
		this.findSyncNodeAndComputeImprovement(this.clusterNode);
		
		Stack<AbstractTreeNode> callpath = new Stack<AbstractTreeNode>();
		callpath.add(this.clusterNode);
		this.generateImprovementReport(callpath, new ArrayList<SignificantCallpath>());
	}
	
	private NodeType getNodeType(AbstractTreeNode node) {
		if (node.getName().toLowerCase().contains("mpi_allgather")) return NodeType.SyncNode;
		if (node.getName().toLowerCase().contains("mpi_allreduce")) return NodeType.SyncNode;
		if (node.getName().toLowerCase().contains("mpi_barrier")) return NodeType.SyncNode;
		
    	if (node.getName().length()>=5 && node.getName().substring(0, 5).equals("PMPI_")) return NodeType.WaitNode;
    	if (node.getName().length()>=4 && node.getName().substring(0, 4).equals("MPI_")) return NodeType.WaitNode;
		
		return NodeType.CompNode;
	}
	
	private NodeType getNodeType(String name) {
		if (name.toLowerCase().contains("mpi_allgather")) return NodeType.SyncNode;
		if (name.toLowerCase().contains("mpi_barrier")) return NodeType.SyncNode;
		
    	if (name.length()>=5 && name.substring(0, 5).equals("PMPI_")) return NodeType.WaitNode;
    	if (name.length()>=4 && name.substring(0, 4).equals("MPI_")) return NodeType.WaitNode;
		
		return NodeType.CompNode;
	}
	
	private long computeAverage(long total, long divider) {
		return (total + divider/2) / divider;
	}
	
	/**
	 * Find sync node:
	 *   if any child of a node has a descendant sync node, that child node will be added to the syncChildNodes set of the node.
	 * 
	 * Compute improvement:
	 * 		Improvement for computation nodes = 	max - avg;
	 * 		Improvement for wait nodes = 		(max - avg) + avg = max;
	 * 		Improvement for synchronization nodes = avg - min;
	 */
	private boolean findSyncNodeAndComputeImprovement(AbstractTreeNode node) {
		NodeType type = getNodeType(node);
		
		if (type == NodeType.SyncNode) {
			long improvement = computeAverage(node.getTotalDurationRep(), node.getWeight()) - node.getMinDurationRep();
			improvement *= (node.getWeight() / numProc);
			node.setInclusiveImprovement(improvement);
			node.setExclusiveImprovement(improvement);
			return true;
		}
		
		if (type == NodeType.WaitNode) {
			long improvement = node.getMaxDurationRep();
			improvement *= (node.getWeight() / numProc);
			node.setInclusiveImprovement(improvement);
			node.setExclusiveImprovement(improvement);
			return false;
		}
		
		// NodeType.compNode
		long improvement = node.getMaxDurationRep() - computeAverage(node.getTotalDurationRep(), node.getWeight());
		improvement *= (node.getWeight() / numProc);
		node.setExclusiveImprovement(improvement);
		
		improvement = 0;
		ArrayList<AbstractTreeNode> syncChildNodes = new ArrayList<AbstractTreeNode>(4);
		
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++) {
				if (findSyncNodeAndComputeImprovement(trace.getChild(i)))
					syncChildNodes.add(trace.getChild(i));
				improvement += trace.getChild(i).getInclusiveImprovement();
			}
		} 
		else if (node instanceof ProfileNode) {
			ProfileNode prof = (ProfileNode) node;
			for (ProfileNode child : prof.getChildMap().values()) { 
				if (findSyncNodeAndComputeImprovement(child))
					syncChildNodes.add(child); 
				improvement += child.getInclusiveImprovement();
			}
		}
		else if (node instanceof ClusterSetNode) {
			ClusterSetNode cluster = (ClusterSetNode) node;
			if (findSyncNodeAndComputeImprovement(cluster.getRep()))
				syncChildNodes.add(cluster.getRep());
			improvement += cluster.getRep().getInclusiveImprovement();
		}
		
		improvement = Math.max(improvement, node.getExclusiveImprovement());
		node.setInclusiveImprovement(improvement);
		
		if (syncChildNodes.size() > 0) {
			this.syncChildNodesMap.put(node, syncChildNodes);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private void generateImprovementReport(Stack<AbstractTreeNode> callpath, ArrayList<SignificantCallpath> significantCallpaths) {
		AbstractTreeNode node = callpath.peek();
		
		NodeType type = getNodeType(node);
		
		if (type == NodeType.SyncNode) {
			double imbalanceImprovementRatio = (double) node.getExclusiveImprovement() / (double) this.totalDuration;
			if (imbalanceImprovementRatio > minimumSyncRatio) {
				double waitImprovementRatio = 0;
				double totalImprovementRatio = imbalanceImprovementRatio;
				for (SignificantCallpath item : significantCallpaths) {
					waitImprovementRatio += item.waitImprovementRatio;
					totalImprovementRatio += item.imbalanceImprovementRatio + item.waitImprovementRatio;
				}
				
				SignificantCallpath syncCallpath = new SignificantCallpath(callpath, imbalanceImprovementRatio, 0, true);
				significantCallpaths.add(syncCallpath); //TODO may not need to show sync node if its imbalanceImprovementRatio is low.
				
				if (totalImprovementRatio > minimumImprovementGroupRatio)
					this.improvementReport.add(new ImprovementGroup(this.lastSyncCallpath, imbalanceImprovementRatio, waitImprovementRatio,
							significantCallpaths));
				
				significantCallpaths.clear();
				this.lastSyncCallpath = (Stack<AbstractTreeNode>) callpath.clone();
			}
			return;
		}
		
		if (type == NodeType.WaitNode) {
			if (node.getExclusiveImprovement() > this.totalDuration * minimumImprovementItemRatio * 2) {
				double waitImprovementRatio = computeAverage(node.getTotalDurationRep(), node.getWeight());
				waitImprovementRatio *= (node.getWeight() / numProc);
				waitImprovementRatio /= (double) this.totalDuration;
				
				double imbalanceImprovementRatio = (double) node.getExclusiveImprovement() / (double) this.totalDuration 
						- waitImprovementRatio;
				
				significantCallpaths.add(new SignificantCallpath(callpath, imbalanceImprovementRatio, waitImprovementRatio, false));
			}
			return;
		}
		
		// NodeType.compNode
		boolean hasSyncChild = this.syncChildNodesMap.containsKey(node);
		
		if (node.getInclusiveImprovement() < this.totalDuration * minimumImprovementItemRatio)
			if (!hasSyncChild)
				return;
		
		// First, determine if visiting children is necessary
		boolean betterChild = false;
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++) {
				if (trace.getChild(i).getInclusiveImprovement() > node.getExclusiveImprovement() * hotpathRatio) {
					betterChild = true;
					break;
				}
			}
		} else if (node instanceof ProfileNode) {
			for (ProfileNode child : ((ProfileNode) node).getChildMap().values())
				if (child.getInclusiveImprovement() > node.getExclusiveImprovement() * hotpathRatio) {
					betterChild = true;
					break;
				}
		} else betterChild = true; // always true for ClusterSetNode
		
		// If not and there is no sync child, 
		if ((!betterChild) && (!hasSyncChild)) {
			double imbalanceImprovementRatio = (double) node.getExclusiveImprovement() / (double) this.totalDuration;
			double waitImprovementRatio = 0;
			significantCallpaths.add(new SignificantCallpath(callpath, imbalanceImprovementRatio, waitImprovementRatio, false));
			return;
		}
		
		// If visiting children is necessary or there is sync child
		ArrayList<AbstractTreeNode> syncChildNodes = this.syncChildNodesMap.get(node);
		
		boolean isLoop = (node instanceof IterationTrace);
		
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			
			/*
			if (hasSyncChild && isLoop) { // if a loop with sync child
				// find the index of the last sync child
				int indexLastSyncChild = 0;
				while (trace.getChild(indexLastSyncChild) != syncChildNodes.get(syncChildNodes.size()-1))
					indexLastSyncChild ++;
				
				// First, visit indexLastSyncChild to include any significant comp/wait node after the sync node
				int sizeReport = this.improvementReport.size();
				Stack<AbstractTreeNode> lastSyncCallpath = this.lastSyncCallpath;
				ArrayList<SignificantCallpath> tempSignificantCallpaths = new ArrayList<SignificantCallpath>();
				
				callpath.push(trace.getChild(indexLastSyncChild));
				generateImprovementReport(callpath, tempSignificantCallpaths);
				callpath.pop();
				
				// Reverse any report generated in this process
				while (this.improvementReport.size() > sizeReport) this.improvementReport.remove(this.improvementReport.size()-1);
				this.lastSyncCallpath = lastSyncCallpath;
				// Append significant comp/wait node after the sync node to the ImproveBreakDown
				significantCallpaths.addAll(tempSignificantCallpaths);
				
				// Visit child from indexLastSyncChild+1 to last element
				for (int i = indexLastSyncChild + 1; i < trace.getNumOfChildren(); i++) {
					callpath.push(trace.getChild(i));
					generateImprovementReport(callpath, significantCallpaths);
					callpath.pop();
				}
				
				// Visit child from 0 to indexLastSyncChild
				for (int i = 0; i <= indexLastSyncChild; i++) {
					callpath.push(trace.getChild(i));
					generateImprovementReport(callpath, significantCallpaths);
					callpath.pop();
				}
				
				// Merge all generated improvement group in this loop
				if (this.improvementReport.size() > sizeReport + 1) {
					this.improvementReport.get(this.improvementReport.size()-2).merge(this.improvementReport.get(this.improvementReport.size()-1));
					this.improvementReport.remove(this.improvementReport.size()-1);
				}
				
				significantCallpaths.clear();
			}*/
			//else { // if not a loop, or a loop with no sync, visit children in control flow order
				for (int i = 0; i < trace.getNumOfChildren(); i++) {
					callpath.push(trace.getChild(i));
					generateImprovementReport(callpath, significantCallpaths);
					callpath.pop();
				}
			//}
		} 
		else if (node instanceof ProfileNode) {
			ProfileNode prof = (ProfileNode) node;
			
			if (prof.getCFGGraph() != null && prof.getCFGGraph().valid) {
				ProfileNode children[] = new ProfileNode[prof.getChildMap().values().size()];
				int i = 0;
				for (ProfileNode child : prof.getChildMap().values())
					children[i++] = child;
				
				for (i = 0; i < children.length; i++)
					for (int j = i+1; j < children.length; j++)
						if (prof.getCFGGraph().compareNodeOrder(children[i], children[j]) > 0) {
							ProfileNode swap = children[i];
							children[i] = children[j];
							children[j] = swap;
						}
				
				for (i = 0; i < children.length; i++) {
					callpath.push(children[i]);
					generateImprovementReport(callpath, significantCallpaths);
					callpath.pop();
				}
			}
			else {
				//if (hasSyncChild && syncChildNodes.size() > 1) System.out.println("error!!!!!!!!!!!!!!");
				int sizeReport = this.improvementReport.size();
				
				for (ProfileNode child : prof.getChildMap().values())
					if ((!hasSyncChild) || child != syncChildNodes.get(syncChildNodes.size()-1)) {
						callpath.push(child);
						generateImprovementReport(callpath, significantCallpaths);
						callpath.pop();
					}
				
				if (hasSyncChild) {
					callpath.push(syncChildNodes.get(syncChildNodes.size()-1));
					generateImprovementReport(callpath, significantCallpaths);
					callpath.pop();
				}
				
				// Merge all generated improvement group into one group
				if (this.improvementReport.size() > sizeReport + 1) {
					this.improvementReport.get(this.improvementReport.size()-2).merge(this.improvementReport.get(this.improvementReport.size()-1));
					this.improvementReport.remove(this.improvementReport.size()-1);
				}
			}
		}
		else if (node instanceof ClusterSetNode) {
			ClusterSetNode cluster = (ClusterSetNode) node;
			
			callpath.push(cluster.getRep());
			generateImprovementReport(callpath, significantCallpaths);
			callpath.pop();
		}
	}
	
	/*
	private String printImprovement(AbstractTreeNode node) {
		if (node.getInclusiveImprovement() < this.totalDuration * minimumImprovementGroupRatio) return "";
		
		// First, determine if child of the node would be better chosen as a significant improvement node to be printed
		boolean betterChild = false;
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++) {
				if (trace.getChild(i).getInclusiveImprovement() > node.getInclusiveImprovement() * hotpathRatio) {
					betterChild = true;
					break;
				}
			}
		} else if (node instanceof ProfileNode) {
			for (ProfileNode child : ((ProfileNode) node).getChildMap().values())
				if (child.getInclusiveImprovement() > node.getInclusiveImprovement() * hotpathRatio) {
					betterChild = true;
					break;
				}
		} else betterChild = true; // always true for ClusterSetNode
		
		if (!betterChild) {
			String str = node.toString(node.getDepth(), 0, numProc);
			str = str.substring(0, str.length()-1);
			
			//double incRatio = (double)node.getInclusiveImprovement() / (double)this.totalDuration * 100;
			double excRatio = (double)node.getExclusiveImprovement() / (double)this.totalDuration * 100;
			
			//str += " inclusive improvement = " + String.format("%.2f", incRatio) + "%";
			str += " improvement = " + String.format("%.2f", excRatio) + "%";
			
			str += "\n";
			
			return str;
		}
		
		String childStr = "";
		
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			for (int i = 0; i < trace.getNumOfChildren(); i++) {
				if (trace.getChild(i).getInclusiveImprovement() > node.getInclusiveImprovement() * minimunChildRatio) {
					childStr += printImprovement(trace.getChild(i));
				}
			}
		} else if (node instanceof ProfileNode) {
			for (ProfileNode child : ((ProfileNode) node).getChildMap().values())
				if (child.getInclusiveImprovement() > node.getInclusiveImprovement() * minimunChildRatio) {
					childStr += printImprovement(child);
				}
		} else if (node instanceof ClusterSetNode) {
			childStr += printImprovement(((ClusterSetNode) node).getRep());
		}
		
		if (childStr.length() > 0) {
			String str = node.toString(node.getDepth(), 0, numProc);
			str = str.substring(0, str.indexOf('\n')+1);
			return str + childStr;
		}
		else return "";
	}*/
	
	long runTime[][];
	long gapTime[][];
	
	ImprovementGroup improvementGroup;
	int procID;
	int callpathID;
	
	private void extractRunTime(AbstractTreeNode node) {
		Stack<AbstractTreeNode> callpath = null;
		if (callpathID == -1) callpath = improvementGroup.priorSyncCallpath;
		else if (callpathID < improvementGroup.callpaths.size()) callpath = improvementGroup.callpaths.get(callpathID).callpath;
		
		// Doesn't match, include runtime in gap
		if (callpath == null || node.getDepth() >= callpath.size() || callpath.get(node.getDepth()).getID() != node.getID()) {
			if (callpathID != -1) gapTime[procID][callpathID] += node.getDuration() * node.getWeight();
			return;
		}
		
		// Node and depth match -- exact node for the corresponding callpath
		if (node.getDepth() == callpath.size() - 1) {
			//System.out.println("Matched at ID " + callpathID + ": " + node.getName() + "(" + node.getID() + ")");
			
			if (callpathID != -1) runTime[procID][callpathID] = node.getDuration() * node.getWeight();
			callpathID++;
			
			return;
		}
		
		// depth not match -- a child of this node will be match the corresponding callapth
		boolean isLoop = false;
		if (node.getCFGGraph() != null) isLoop = node.getCFGGraph() instanceof CFGLoop;
		else isLoop = node.getName().contains("loop at");
		
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			
			if (isLoop) {
				if (callpathID == -1) {
					callpathID = 0;
					return;
				}
				
				int firstCallpathID = callpathID;
				int lastCallpathID = callpathID + 1;
				while (lastCallpathID < improvementGroup.callpaths.size() && 
						improvementGroup.callpaths.get(lastCallpathID).callpath.get(node.getDepth()).getID() == node.getID()) {
					lastCallpathID ++;
				}
				lastCallpathID --;
				
				callpathID = lastCallpathID;
				int childIdx = 0;
				for (; childIdx < trace.getNumOfChildren(); childIdx++)
					if (trace.getChild(childIdx).getID() == improvementGroup.callpaths.get(lastCallpathID).callpath.get(node.getDepth()+1).getID()) {
						extractRunTime(trace.getChild(childIdx));
						break;
					}
				
				gapTime[procID][firstCallpathID] += gapTime[procID][lastCallpathID+1];
				gapTime[procID][lastCallpathID] = 0;
				gapTime[procID][lastCallpathID+1] = 0;
				
				callpathID = firstCallpathID;
				boolean callpathInLoop = true;
				for (int i = childIdx + 1; i < trace.getNumOfChildren(); i++) {
					gapTime[procID][callpathID] += trace.getGapDurationBeforeChild(i) * trace.getWeight();
					extractRunTime(trace.getChild(i));
				}
				gapTime[procID][callpathID] += trace.getGapDurationBeforeChild(trace.getNumOfChildren()) * trace.getWeight();
				for (int i = 0; i <= childIdx; i++) {
					if (callpathID != firstCallpathID) {
						firstCallpathID = callpathID;
						callpath = null;
						if (callpathID < improvementGroup.callpaths.size()) callpath = improvementGroup.callpaths.get(callpathID).callpath;
						if (callpath == null || node.getDepth() >= callpath.size() || callpath.get(node.getDepth()).getID() != node.getID())
							callpathInLoop = false;
					}
					if (callpathInLoop && trace.getCFGGraph() != null && trace.getCFGGraph().valid && 
							trace.getCFGGraph().compareNodeOrder(trace.getChild(i), callpath.get(trace.getDepth()+1)) > 0) {
						//System.out.println("Skipped at ID " + callpathID);
						callpathID++;
						i--;
						continue;
					}
					gapTime[procID][callpathID] += trace.getGapDurationBeforeChild(i) * trace.getWeight();
					extractRunTime(trace.getChild(i));
				}
				
				gapTime[procID][callpathID] = 0;
			}
			else {
				int firstCallpathID = callpathID;
				boolean callpathInLoop = true;
				for (int i = 0; i < trace.getNumOfChildren(); i++) {
					if (callpathID != firstCallpathID) {
						firstCallpathID = callpathID;
						callpath = null;
						if (callpathID < improvementGroup.callpaths.size()) callpath = improvementGroup.callpaths.get(callpathID).callpath;
						
						if (callpath == null) return; // terminate TODO
						if (node.getDepth() >= callpath.size() || callpath.get(node.getDepth()).getID() != node.getID())
							callpathInLoop = false;
					}
					if (callpathInLoop && trace.getCFGGraph() != null && trace.getCFGGraph().valid && 
							trace.getCFGGraph().compareNodeOrder(trace.getChild(i), callpath.get(trace.getDepth()+1)) > 0) {
						//System.out.println("Skipped at ID " + callpathID);
						callpathID++;
						i--;
						continue;
					}
					if (callpathID != -1) gapTime[procID][callpathID] += trace.getGapDurationBeforeChild(i) * trace.getWeight();
					extractRunTime(trace.getChild(i));
				}
				if (callpathID != -1) gapTime[procID][callpathID] += trace.getGapDurationBeforeChild(trace.getNumOfChildren()) * trace.getWeight();
			}
		}
		else if (node instanceof ProfileNode) { //TODO not finished
			ProfileNode prof = (ProfileNode) node;
			if (callpathID != -1) gapTime[procID][callpathID] += (prof.getMinDurationExclusive() + prof.getMaxDurationExclusive()) / 2 * prof.getWeight();
			for (ProfileNode child : prof.getChildMap().values())
				extractRunTime(child);
		}
		else if (node instanceof ClusterSetNode) {
			ClusterSetNode cluster = (ClusterSetNode) node;
			extractRunTime(cluster.getRep());
		}
		
	}
	
	
	private String callpathToString(Stack<AbstractTreeNode> callpath, Stack<AbstractTreeNode> lastCallpath, String indent, String append) {
		String ret = "";
		
		int idx = 0;
		while (idx < callpath.size() && idx < lastCallpath.size() && callpath.get(idx) == lastCallpath.get(idx))
			idx++;
		
		for (int i = 0; i < callpath.get(idx).getDepth(); i++) indent += "  ";
		
		for (int i = idx; i < callpath.size() - 1; i++) {
			ret += indent + callpath.get(i).getName() + "(" + callpath.get(i).getID() + ")\n";
			indent += "  ";
		}
		ret += indent + "**" + callpath.peek().getName() + "(" + callpath.peek().getID() + ")" + append + "\n";
		
		return ret;
	}
	
	private String callpathToString(Stack<String> callpath, Stack<Boolean> allPresent, Stack<String> lastCallpath, String indent, String append) {
		String ret = "";
		
		int idx = 0;
		while (idx < callpath.size() && idx < lastCallpath.size() && callpath.get(idx).equals(lastCallpath.get(idx)))
			idx++;
		
		for (int i = 0; i < idx; i++) indent += "  ";
		
		for (int i = idx; i < callpath.size() - 1; i++) {
			if (allPresent.get(i) == true)
				ret += indent + callpath.get(i) + "\n";
			else 
				ret += indent + "(" + callpath.get(i) + ")\n";
			indent += "  ";
		}
		ret += indent + "**" + callpath.peek() + append + "\n";
		
		return ret;
	}
	
	public void printImprovementReport() {
		AbstractTraceNode origin = clusterNode.getOrigin();
		
		objPrint.println("Detailed Report:");
		
		for (int k = 0; k < this.improvementReport.size(); k++) {
			ImprovementGroup group = this.improvementReport.get(k);
			String str = "Group #" + k + ":  imbalance = " + String.format("%.2f", group.imbalanceImprovementRatio * 100) + "%  " + 
					"wait = " + String.format("%.2f", group.waitImprovementRatio * 100) + "%\n";
			
			int countCause = 0;
			int countSync = 0;
			
			Stack<AbstractTreeNode> lastCallPath = new Stack<AbstractTreeNode>();
			if (group.priorSyncCallpath != null) {
				str += this.callpathToString(group.priorSyncCallpath, lastCallPath, "  ", " ** PRIOR **");
				lastCallPath = group.priorSyncCallpath;
			}
			for (SignificantCallpath item : group.callpaths) {
				String temp = "";
				
				if (item.isSync) temp += "  Sync #" + (++countSync);
				else temp += "  Cause #" + (++countCause);
				
				temp += ":  imbalance = " + String.format("%.2f", item.imbalanceImprovementRatio * 100) + "%  " + 
						"wait = " + String.format("%.2f", item.waitImprovementRatio * 100) + "%";
				str += this.callpathToString(item.callpath, lastCallPath, "  ", temp);
				lastCallPath = item.callpath;
			}

			str += "\n\n";
			
			objPrint.print(str);
			
			/*
			runTime = new long [numProc][group.callpaths.size()];
			gapTime = new long [numProc][group.callpaths.size()+1];
			improvementGroup = group;
			
			for (int i = 0; i < numProc; i++) {
				procID = i;
				callpathID = (group.priorSyncCallpath == null) ? 0 : -1;
				extractRunTime(origin.getChild(procID));
			}
			
			objPrint.print(" \t");
			countCause = 0;
			countSync = 0;
			for (SignificantCallpath item : group.callpaths)
				if (item.isSync) objPrint.print("gap\t Sync #" + (++countSync) +"\t");
				else objPrint.print("gap\t Cause #" + (++countCause) +"\t");
			objPrint.println("gap");
			
			
			for (int i = 0; i < numProc; i++) {
				objPrint.print("Proc #" + i + "\t");
				for (int j = 0; j < group.callpaths.size(); j++)
					objPrint.print(gapTime[i][j] + "\t" + runTime[i][j] + "\t");
				objPrint.println(gapTime[i][group.callpaths.size()]);
			}
			
			objPrint.println("\n\n");
			*/
		}
		
		objPrint.println("\n\nMerged Report:");
		for (int k = 0; k < this.improvementReport.size(); k++) {
			ImprovementGroup group = this.improvementReport.get(k);
			String str = "Group #" + k + ":  imbalance = " + String.format("%.2f", group.imbalanceImprovementRatio * 100) + "%  " + 
					"wait = " + String.format("%.2f", group.waitImprovementRatio * 100) + "%\n";
			
			int countCause = 0;
			int countSync = 0;
			
			Stack<String> lastCallPath = new Stack<String>();
			if (group.priorSyncCallpath != null) {
				Stack<Boolean> allPresent = new Stack<Boolean>();
				for (AbstractTreeNode node : group.priorSyncCallpath) {
					lastCallPath.add(node.getName());
					allPresent.add(true);
				}
				str += this.callpathToString(lastCallPath, allPresent, new Stack<String>(), "  ", " ** PRIOR **");
			}
			
			ArrayList<MergedCallpath> callpaths = new ArrayList<MergedCallpath>();
			for (SignificantCallpath item : group.callpaths) callpaths.add(new MergedCallpath(item));
			this.mergeCallPaths(callpaths);
			
			for (MergedCallpath item : callpaths) {
				String temp = "";
				
				if (item.isSync) temp += "  Sync #" + (++countSync);
				else temp += "  Cause #" + (++countCause);
				
				temp += ":  imbalance = " + String.format("%.2f", item.imbalanceImprovementRatio * 100) + "%  " + 
						"wait = " + String.format("%.2f", item.waitImprovementRatio * 100) + "%";
				str += this.callpathToString(item.callpath, item.allPresent, lastCallPath, "  ", temp);
				lastCallPath = item.callpath;
			}

			str += "\n\n";
			
			objPrint.print(str);
		}
	}

	private void mergeCallPaths(ArrayList<MergedCallpath> callpaths) {
		for (int i = 1; i < callpaths.size(); i++) {
			for (int k = i-1; k >=0; k--)
				if (this.getNodeType(callpaths.get(i).callpath.peek()) == NodeType.CompNode) {
					if (callpaths.get(i).callpath.peek().equals(callpaths.get(k).callpath.peek())) { // ending with the same function
						MergedCallpath merged = new MergedCallpath(callpaths.get(k), callpaths.get(i));
						if (merged.callpath.size() <= Math.max(callpaths.get(i).callpath.size(), callpaths.get(k).callpath.size())*1.25) {
							callpaths.set(k, merged);
							callpaths.remove(i);
							i--;
							break;
						}
					}
				} else {
					if (callpaths.get(i).callpath.peek().equals(callpaths.get(k).callpath.peek()) &&
							callpaths.get(i).callpath.get(callpaths.get(i).callpath.size()-2).equals
							(callpaths.get(k).callpath.get(callpaths.get(k).callpath.size()-2))) { // ending with the same last 2 functions
						MergedCallpath merged = new MergedCallpath(callpaths.get(k), callpaths.get(i));
						if (merged.callpath.size() <= Math.max(callpaths.get(i).callpath.size(), callpaths.get(k).callpath.size())*1.25) {
							callpaths.set(k, merged);
							callpaths.remove(i);
							i--;
							break;
						}
					}
				}
		}
	}
	
	public int[][] getSignificantCallpaths() {
		int[][] callpaths = null;
		
		int numCallpaths = 0;
		for (int k = 0; k < this.improvementReport.size(); k++) 
			numCallpaths += improvementReport.get(k).callpaths.size();
		
		callpaths = new int [numCallpaths][];
		
		int i = 0;
		for (ImprovementGroup group : this.improvementReport)
			for (SignificantCallpath callpath : group.callpaths) {
				callpaths[i] = callpath.toArray();
				i++;
			}
		
		return callpaths;
	}
}

enum NodeType {
	CompNode,
	WaitNode,
	SyncNode
}

class ImprovementGroup {
	Stack<AbstractTreeNode> priorSyncCallpath;
	
	double imbalanceImprovementRatio;
	double waitImprovementRatio;
	
	ArrayList<SignificantCallpath> callpaths;

	@SuppressWarnings("unchecked")
	public ImprovementGroup(Stack<AbstractTreeNode> priorSyncCallpath, double imbalanceImprovementRatio, double waitImprovementRatio,
			ArrayList<SignificantCallpath> callpaths) {
		this.priorSyncCallpath = (priorSyncCallpath == null) ? null : (Stack<AbstractTreeNode>) priorSyncCallpath.clone();
		this.imbalanceImprovementRatio = imbalanceImprovementRatio;
		this.waitImprovementRatio = waitImprovementRatio;
		this.callpaths = (ArrayList<SignificantCallpath>) callpaths.clone();
	}
	
	public void merge(ImprovementGroup other) {
		this.imbalanceImprovementRatio += other.imbalanceImprovementRatio;
		this.waitImprovementRatio += other.waitImprovementRatio;
		this.callpaths.addAll(other.callpaths);
	}
}

class SignificantCallpath {
	Stack<AbstractTreeNode> callpath;
	double imbalanceImprovementRatio;
	double waitImprovementRatio;
	boolean isSync;
	
	@SuppressWarnings("unchecked")
	public SignificantCallpath(Stack<AbstractTreeNode> callpath,
			double imbalanceImprovementRatio, double waitImprovementRatio, boolean isSync) {
		this.callpath = (Stack<AbstractTreeNode>) callpath.clone();
		this.imbalanceImprovementRatio = imbalanceImprovementRatio;
		this.waitImprovementRatio = waitImprovementRatio;
		this.isSync = isSync;
	}
	
	int[] toArray() {
		int[] ret = new int[callpath.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = callpath.get(i).getID();
		return ret;
	}
}

class MergedCallpath {
	Stack<String> callpath;
	Stack<Boolean> allPresent;
	double imbalanceImprovementRatio;
	double waitImprovementRatio;
	boolean isSync;
	
	public MergedCallpath(SignificantCallpath origin) {
		this.callpath = new Stack<String>();
		this.allPresent = new Stack<Boolean>();
		for (AbstractTreeNode node : origin.callpath) {
			callpath.add(node.getName());
			allPresent.add(true);
		}
		
		this.imbalanceImprovementRatio = origin.imbalanceImprovementRatio;
		this.waitImprovementRatio = origin.waitImprovementRatio;
		this.isSync = origin.isSync;
	}
	
	public MergedCallpath(MergedCallpath p1, MergedCallpath p2) {
		this.callpath = new Stack<String>();
		this.allPresent = new Stack<Boolean>();
		
		int i = 0, j = 0;
		while (i < p1.callpath.size() && j < p2.callpath.size()) {
			if (p1.callpath.get(i).equals(p2.callpath.get(j))) {
				this.callpath.add(p1.callpath.get(i));
				this.allPresent.add(p1.allPresent.get(i) & p2.allPresent.get(j));
				i++;j++;
			} else {
				boolean found = false;
				for (int k = j+1; k < p2.callpath.size(); k++)
					if (p1.callpath.get(i).equals(p2.callpath.get(k))) {
						found = true;
						break;
					}
				
				if (found) {
					this.callpath.add(p2.callpath.get(j));
					this.allPresent.add(false);
					j++;
				} else {
					this.callpath.add(p1.callpath.get(i));
					this.allPresent.add(false);
					i++;
				}
			}
		}
		
		this.imbalanceImprovementRatio = p1.imbalanceImprovementRatio + p2.imbalanceImprovementRatio;
		this.waitImprovementRatio = p1.waitImprovementRatio + p2.waitImprovementRatio;
		this.isSync = p1.isSync & p2.isSync;
	}
}