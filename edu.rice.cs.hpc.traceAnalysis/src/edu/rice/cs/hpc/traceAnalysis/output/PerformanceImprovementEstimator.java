package edu.rice.cs.hpc.traceAnalysis.output;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGLoop;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterSetNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ProfileNode;

public class PerformanceImprovementEstimator {
	
	static private final double minimumImprovementGroupRatio = 0.01;
	static private final double minimumImprovementItemRatio = 0.003;
	
	static private final double hotpathRatio = 0.2;
	static private final double minimunChildRatio = 0.02;
	
	private final PrintStream objPrint;
	private final ClusterSetNode clusterNode;
	
	private final long totalDuration;
	private final int numProc;
	
	private HashMap<AbstractTreeNode, ArrayList<AbstractTreeNode>> syncChildNodesMap;
	private ArrayList<ImprovementGroup> improvementReport;
	
	
	private PerformanceImprovementEstimator(PrintStream objPrint, ClusterSetNode clusterNode) {
		this.objPrint = objPrint;
		this.clusterNode = clusterNode;
		
		this.numProc = this.clusterNode.getRep().getWeight();
		this.totalDuration = this.clusterNode.getDuration();
		
		this.syncChildNodesMap = new HashMap<AbstractTreeNode, ArrayList<AbstractTreeNode>>();
		this.improvementReport = new ArrayList<ImprovementGroup>();
		
		this.findSyncNodeAndComputeImprovement(this.clusterNode);
		
		Stack<AbstractTreeNode> callpath = new Stack<AbstractTreeNode>();
		callpath.add(this.clusterNode);
		this.generateImprovementReport(callpath, new ArrayList<ImprovementItem>());
	}
	
	private NodeType getNodeType(AbstractTreeNode node) {
		if (node.getName().contains("PMPI_Allgather")) return NodeType.SyncNode;
		if (node.getName().contains("PMPI_Barrier")) return NodeType.SyncNode;
		
		if (node.getName().contains("PMPI_Waitall")) return NodeType.WaitNode;
		
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
			//TODO need to sort nodes in syncChildNodes according to control flow
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
	
	private void generateImprovementReport(Stack<AbstractTreeNode> callpath, ArrayList<ImprovementItem> improveBreakDown) {
		AbstractTreeNode node = callpath.peek();
		
		NodeType type = getNodeType(node);
		
		if (type == NodeType.SyncNode) {
			double imbalanceImprovementRatio = (double) node.getExclusiveImprovement() / (double) this.totalDuration;
			double waitImprovementRatio = 0;
			for (ImprovementItem item : improveBreakDown)
				waitImprovementRatio += item.waitImprovementRatio;
			
			if (imbalanceImprovementRatio + waitImprovementRatio > minimumImprovementGroupRatio)
				this.improvementReport.add(new ImprovementGroup(callpath, imbalanceImprovementRatio, waitImprovementRatio,
						improveBreakDown));
			
			improveBreakDown.clear();
			return;
		}
		
		if (type == NodeType.WaitNode) {
			if (node.getExclusiveImprovement() > this.totalDuration * minimumImprovementItemRatio) {
				double waitImprovementRatio = computeAverage(node.getTotalDurationRep(), node.getWeight());
				waitImprovementRatio *= (node.getWeight() / numProc);
				waitImprovementRatio /= (double) this.totalDuration;
				
				double imbalanceImprovementRatio = (double) node.getExclusiveImprovement() / (double) this.totalDuration 
						- waitImprovementRatio;
				
				improveBreakDown.add(new ImprovementItem(callpath, imbalanceImprovementRatio, waitImprovementRatio));
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
		
		// If not and there is no sync child, 
		if ((!betterChild) && (!hasSyncChild)) {
			double imbalanceImprovementRatio = (double) node.getExclusiveImprovement() / (double) this.totalDuration;
			double waitImprovementRatio = 0;
			improveBreakDown.add(new ImprovementItem(callpath, imbalanceImprovementRatio, waitImprovementRatio));
			return;
		}
		
		// If visiting children is necessary or there is sync child
		ArrayList<AbstractTreeNode> syncChildNodes = this.syncChildNodesMap.get(node);
		
		boolean isLoop = false;
		if (node.getCFGGraph() != null)
			isLoop = node.getCFGGraph() instanceof CFGLoop;
		else
			isLoop = node.getName().contains("loop at");
		
		if (node instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) node;
			
			if (hasSyncChild && isLoop) { // if a loop with sync child
				// find the index of the last sync child
				int indexLastSyncChild = 0;
				while (trace.getChild(indexLastSyncChild) != syncChildNodes.get(syncChildNodes.size()-1))
					indexLastSyncChild ++;
				
				// First, visit indexLastSyncChild to include any significant comp/wait node after the sync node
				int sizeReport = this.improvementReport.size();
				ArrayList<ImprovementItem> tempImproveBreakDown = new ArrayList<ImprovementItem>();
				
				callpath.push(trace.getChild(indexLastSyncChild));
				generateImprovementReport(callpath, tempImproveBreakDown);
				callpath.pop();
				
				// Reverse any report generated in this process
				while (this.improvementReport.size() > sizeReport) this.improvementReport.remove(this.improvementReport.size()-1);
				// Append significant comp/wait node after the sync node to the ImproveBreakDown
				improveBreakDown.addAll(tempImproveBreakDown);
				
				// Visit child from indexLastSyncChild+1 to last element
				for (int i = indexLastSyncChild + 1; i < trace.getNumOfChildren(); i++) {
					callpath.push(trace.getChild(i));
					generateImprovementReport(callpath, improveBreakDown);
					callpath.pop();
				}
				
				// Visit child from 0 to indexLastSyncChild
				for (int i = 0; i <= indexLastSyncChild; i++) {
					callpath.push(trace.getChild(i));
					generateImprovementReport(callpath, improveBreakDown);
					callpath.pop();
				}
				
				improveBreakDown.clear();
			}
			else { // if not a loop, or a loop with no sync, visit children in control flow order
				for (int i = 0; i < trace.getNumOfChildren(); i++) {
					callpath.push(trace.getChild(i));
					generateImprovementReport(callpath, improveBreakDown);
					callpath.pop();
				}
			}
		} 
		else if (node instanceof ProfileNode) { //TODO not finished
			ProfileNode prof = (ProfileNode) node;
			if (hasSyncChild && syncChildNodes.size() > 1) System.out.println("error!!!!!!!!!!!!!!");
			for (ProfileNode child : prof.getChildMap().values())
				if ((!hasSyncChild) || child != syncChildNodes.get(0)) {
					callpath.push(child);
					generateImprovementReport(callpath, improveBreakDown);
					callpath.pop();
				}
			
			if (hasSyncChild) {
				callpath.push(syncChildNodes.get(0));
				generateImprovementReport(callpath, improveBreakDown);
				callpath.pop();
			}
			//TODO need to sort nodes in syncChildNodes according to control flow
		}
		else if (node instanceof ClusterSetNode) {
			ClusterSetNode cluster = (ClusterSetNode) node;
			
			callpath.push(cluster.getRep());
			generateImprovementReport(callpath, improveBreakDown);
			callpath.pop();
		}
	}
	
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
	
	private void printImprovementReport() {
		for (int k = 0; k < this.improvementReport.size(); k++) {
			ImprovementGroup group = this.improvementReport.get(k);
			String str = "Group #" + k + ":  imbalance = " + String.format("%.2f", group.imbalanceImprovementRatio * 100) + "%  " + 
					"wait = " + String.format("%.2f", group.waitImprovementRatio * 100) + "%\n";
			Stack<AbstractTreeNode> lastCallPath = new Stack<AbstractTreeNode>();
			for (int i = 0; i < group.improveBreakdown.size(); i++) {
				ImprovementItem item = group.improveBreakdown.get(i);
				String temp = "  Cause #" + i + ":  imbalance = " + String.format("%.2f", item.imbalanceImprovementRatio * 100) + "%  " + 
						"wait = " + String.format("%.2f", item.waitImprovementRatio * 100) + "%";
				str += this.callpathToString(item.callpath, lastCallPath, "  ", temp);
				lastCallPath = item.callpath;
			}
			
			str += this.callpathToString(group.syncCallpath, lastCallPath, "  ", "  ***** Synchronization *****");
			
			str += "\n\n";
			
			objPrint.print(str);
		}
	}
	
	public static void printSignificantImprovement(PrintStream objPrint, ClusterSetNode node) {
		PerformanceImprovementEstimator printer = new PerformanceImprovementEstimator(objPrint, node);
		printer.printImprovementReport();
		//objPrint.print(printer.printImprovement(node));
	}
}

enum NodeType {
	CompNode,
	WaitNode,
	SyncNode
}

class ImprovementGroup {
	Stack<AbstractTreeNode> syncCallpath;
	double imbalanceImprovementRatio;
	double waitImprovementRatio;
	
	ArrayList<ImprovementItem> improveBreakdown;

	@SuppressWarnings("unchecked")
	public ImprovementGroup(Stack<AbstractTreeNode> syncCallpath,
			double imbalanceImprovementRatio, double waitImprovementRatio,
			ArrayList<ImprovementItem> improveBreakdown) {
		this.syncCallpath = (Stack<AbstractTreeNode>) syncCallpath.clone();
		this.imbalanceImprovementRatio = imbalanceImprovementRatio;
		this.waitImprovementRatio = waitImprovementRatio;
		this.improveBreakdown = (ArrayList<ImprovementItem>) improveBreakdown.clone();
	}
}

class ImprovementItem {
	Stack<AbstractTreeNode> callpath;
	double imbalanceImprovementRatio;
	double waitImprovementRatio;
	
	@SuppressWarnings("unchecked")
	public ImprovementItem(Stack<AbstractTreeNode> callpath,
			double imbalanceImprovementRatio, double waitImprovementRatio) {
		this.callpath = (Stack<AbstractTreeNode>) callpath.clone();
		this.imbalanceImprovementRatio = imbalanceImprovementRatio;
		this.waitImprovementRatio = waitImprovementRatio;
	}
}
