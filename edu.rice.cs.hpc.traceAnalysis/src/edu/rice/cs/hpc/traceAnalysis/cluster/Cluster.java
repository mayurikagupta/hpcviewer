package edu.rice.cs.hpc.traceAnalysis.cluster;

import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;

public class Cluster {
	private AbstractTreeNode rep; // representative
	private Vector<ClusterMemberID> members = new Vector<ClusterMemberID>();
	
	public Cluster(AbstractTreeNode node, int id) {
		this.rep = node.duplicate();
		this.members.add(new ClusterMemberID(id));
	}
	
	public Cluster(AbstractTreeNode node, Vector<ClusterMemberID> members) {
		this.rep = node.duplicate();
		this.members.addAll(members);
	}
	
	public Cluster(Cluster other) {
		this.rep = other.rep.duplicate();
		this.members.addAll(other.members);
	}
	
	public AbstractTreeNode getRep() {
		return rep;
	}
	
	public int getNumOfMembers() {
		return members.size();
	}
	
	public Vector<ClusterMemberID> getMembers() {
		return members;
	}
	
	public void addMembers(Vector<ClusterMemberID> newMembers) {
		members.addAll(newMembers);
	}
	
	public String toString() {
		return "Members in cluster : " + members.toString();
	}
	
	public class ClusterMemberID {
		private String ID;
		
		public ClusterMemberID(int ID) {
			this.ID = Integer.toString(ID);
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof ClusterMemberID) {
				return ID.equals(((ClusterMemberID)obj).ID);
			}
			return false;
		}
		
		public String toString() {
			return ID;
		}
	}
}

