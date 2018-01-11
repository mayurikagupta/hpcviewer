package edu.rice.cs.hpc.traceAnalysis.output;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.tree.*;

public class RuntimeExtractor {
	SignificantCalltreeNode rootNode;
	
	int numProc;
	
	/**
	 * 1st dimension - pathID;
	 * 2nd dimension - iterationNumber;
	 * 3rd dimension - procID;
	 */
	Vector<Vector<Time[]>> table;
	
	long[] endTime;
	
	int[] callpaths;
	
	Vector<HashSet<Integer>> cluster;
	
	public RuntimeExtractor(int numProc, int[][] callpaths, Vector<HashSet<Integer>> cluster) {
		this.numProc = numProc;
		this.cluster = cluster;
		this.callpaths = new int[callpaths.length];
		
		table = new Vector<Vector<Time[]>>(callpaths.length);
		for (int i = 0; i < callpaths.length; i++)
			table.add(new Vector<Time[]>());
		endTime = new long [numProc];
		
		buildSignificantCalltree(callpaths);
	}
	
	void buildSignificantCalltree(int[][] callpaths) {
		for (int i = 0; i < callpaths.length; i++) {
			//for (int j = 0; j < callpaths[i].length; j++)
			//	System.out.print(callpaths[i][j] + " ");
			//System.out.println();
			this.callpaths[i] = callpaths[i][callpaths[i].length-1];
		}
		
		
		rootNode = new SignificantCalltreeNode(null, 0, 0);

		for (int i = 0; i < callpaths.length; i++) {
			SignificantCalltreeNode currentNode = rootNode;
			for (int j = 0; j < callpaths[i].length; j++)
				if (currentNode.nodeID == callpaths[i][j]) continue;
				else if (currentNode.children.containsKey(callpaths[i][j]))
					currentNode = currentNode.children.get(callpaths[i][j]);
				else {
					SignificantCalltreeNode newNode = new SignificantCalltreeNode(currentNode, callpaths[i][j], i);
					currentNode = newNode;
				}
		}
		
		//System.out.println();
		//System.out.println();
		//System.out.println(rootNode.toString(""));
	}
	
	private void extractTime(int procID, AbstractTreeNode treeNode, SignificantCalltreeNode sigNode, int iterID, long time) {
		if (sigNode.isLeaf()) {
			long startTime, endTime;
			if (treeNode.getTraceTime() != null) {
				startTime = treeNode.getTraceTime().getStartTimeInclusive();
				endTime = treeNode.getTraceTime().getEndTimeExclusive();
			}
			else {
				startTime = time;
				endTime = time + treeNode.getDuration();
			}
			
			Vector<Time[]> pathTable = table.get(sigNode.pathID);
			
			// Actually, the while loop doesn't need to be synchronized, in which case it may allocate more entries in the vector than necessary
			synchronized (this) {
				while (pathTable.size() <= iterID) {
					pathTable.add(new Time[numProc]);
				}
			}
			
			if (pathTable.get(iterID)[procID] != null) System.err.println("ERROR when extracting time.");
			pathTable.get(iterID)[procID] = new Time(startTime, endTime);
			return;
		}
		
		if (treeNode instanceof IteratedLoopTrace) {
			IteratedLoopTrace loop = (IteratedLoopTrace)treeNode;
			for (int i = 0; i < loop.getNumOfChildren(); i++)
				extractTime(procID, loop.getChild(i), sigNode, i, 0); // TODO iterID for nested loops
		}
		else if (treeNode instanceof AbstractTraceNode) {
			AbstractTraceNode trace = (AbstractTraceNode) treeNode;
			for (int i = 0; i < trace.getNumOfChildren(); i++)
				if (sigNode.isChild(trace.getChild(i).getID()))
					extractTime(procID, trace.getChild(i), sigNode.getChild(trace.getChild(i).getID()), iterID, 0);
		}
		else { // ProfileNode
			ProfileNode profile = (ProfileNode) treeNode;
			if (profile.getTraceTime() != null)
				time = profile.getTraceTime().getStartTimeInclusive();
			
			for (ProfileNode child : profile.getChildMap().values()) {
				if (sigNode.isChild(child.getID()))
					extractTime(procID, child, sigNode.getChild(child.getID()), iterID, time);
				time += child.getDuration();
			}
		}
	}
	
	public void extractTime(int procID, RootTrace rootTrace) {
		extractTime(procID, rootTrace, rootNode, 0, 0);
		endTime[procID] = rootTrace.getTraceTime().getEndTimeExclusive();
	}
	
	public boolean swapCIPair(CIPair first, CIPair second) {
		for (int k = 0; k < numProc; k++) 
			if (table.get(first.callpathID).get(first.iterID)[k] != null
					&& table.get(second.callpathID).get(second.iterID)[k] != null
					&& table.get(first.callpathID).get(first.iterID)[k].startTime 
					    > table.get(second.callpathID).get(second.iterID)[k].startTime)
			return true;
		return false;
	}
	
	public void printTime(PrintStream objPrint) {
		Vector<CIPair> pair = new Vector<CIPair>();
		for (int i = 0; i < table.size(); i++)
			for (int j = 0; j < table.get(i).size(); j++) {
				for (int k = 0; k < numProc; k++)
					if (table.get(i).get(j)[k] != null) {
						pair.add(new CIPair(i, j));
						break;
					}
			}
		
		for (int i = 0; i < pair.size(); i++)
			for (int j = i+1; j < pair.size(); j++)
				if (swapCIPair(pair.get(i), pair.get(j))) {
					CIPair temp = pair.get(i);
					pair.set(i, pair.get(j));
					pair.set(j, temp);
				}
		
		/*
		for (int i = 0; i < pair.size(); i++) {
			objPrint.print(pair.get(i).toString() + ": ");
			objPrint.print(table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[0] == null ? "none" : 
				table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[0].toString());
			objPrint.print("     ");
			objPrint.println(table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[1] == null ? "none" : 
				table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[1].toString());
		}*/
		
		objPrint.print("Proc\t");
		for (int i = 0; i < pair.size(); i++)
			objPrint.print("gap\t" + pair.get(i).toString()+"\t");
		objPrint.println("gap");
		
		for (int m = 0; m < cluster.size(); m++)
			for (int k = 0; k < numProc; k++) {
				if (cluster.get(m).contains(k)) {
					String output = "";
					output += "P" + k + "\t";
					long lastTime = 0;
					for (int i = 0; i < pair.size(); i++)
						if (table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[k] != null) {
							output += (table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[k].startTime - lastTime)/1000000.0 + "\t";
							output += (table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[k].endTime - table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[k].startTime)/1000000.0 + "\t";
							lastTime = table.get(pair.get(i).callpathID).get(pair.get(i).iterID)[k].endTime;
						} else {
							output += "0\t0\t";
						}
					output += (endTime[k] - lastTime) / 1000000.0;
					
					for (int i = 0; i < numProc / cluster.get(m).size(); i++)
						objPrint.println(output);
				}
			}
	}
		
	
	class Time {
		long startTime;
		long endTime;
		
		Time(long startTime, long endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}
		
		public String toString() {
			return startTime + " ~ " + endTime;
		}
	}
	
	class CIPair {
		int callpathID;
		int iterID;
		
		CIPair(int callpathID, int iterID) {
			this.callpathID = callpathID;
			this.iterID = iterID;
		}
		
		public String toString() {
			//return "Callpath #" + callpathID + " Iteration #" + iterID;
			return "C" + callpaths[callpathID] + "_I" + iterID;
		}
	}
	
	class SignificantCalltreeNode {
		int nodeID;
		int pathID;
		SignificantCalltreeNode parent;
		HashMap<Integer, SignificantCalltreeNode> children;
		
		int numInstance = 0;
		
		SignificantCalltreeNode(SignificantCalltreeNode parent, int NodeID, int pathID) {
			this.parent = parent;
			if (this.parent != null) this.parent.children.put(NodeID, this);
			this.nodeID = NodeID;
			this.pathID = pathID;
			this.children = new HashMap<Integer, SignificantCalltreeNode>();
		}
		
		public String toString(String prior) {
			String myStr = prior + nodeID + " ";
			
			if (isLeaf()) return pathID + ": " + myStr + "\n";
			
			String ret = "";
			for (SignificantCalltreeNode child : children.values())
				ret += child.toString(myStr);
			
			return ret;
		}
		
		public boolean isChild(int ID) {
			return children.containsKey(ID);
		}
		
		public SignificantCalltreeNode getChild(int ID) {
			return children.get(ID);
		}
		
		boolean isLeaf() {
			return this.children.size() == 0;
		}
	}
}
