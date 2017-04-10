package edu.rice.cs.hpc.traceAnalysis.cluster;

import java.util.Collections;
import java.util.Vector;

import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;

public class Cluster {
	private AbstractTreeNode rep; // representative
	private Vector<ClusterMemberID> members = new Vector<ClusterMemberID>();
	
	public Cluster(AbstractTreeNode node, int id) {
		this.rep = node.duplicate();
		if (this.rep instanceof AbstractTraceNode) {
			((AbstractTraceNode) this.rep).shiftTime(-((AbstractTraceNode) this.rep).getTime().getStartTimeExclusive());
		}
		this.members.add(new ClusterMemberID(id));
	}
	
	public Cluster(AbstractTreeNode node, Vector<ClusterMemberID> members) {
		this.rep = node.duplicate();
		this.members.addAll(members);
	}
	
	public Cluster(Cluster other) {
		this.rep = other.rep.duplicate();
		for (ClusterMemberID otherMember : other.members)
			this.members.add(otherMember.duplicate());
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
	
	public void addLabel(int ID) {
		for (ClusterMemberID member : members) 
			member.append(ID);
	}
	
	public String toString() {
		Collections.sort(members);
		return "Members in cluster : " + members.toString();
	}
	
	public class ClusterMemberID implements Comparable<ClusterMemberID> {
		private Vector<Integer> IDs = new Vector<Integer>();
		
		public ClusterMemberID(int ID) {
			IDs.add(ID);
		}
		
		public ClusterMemberID(ClusterMemberID other) {
			IDs.addAll(other.IDs);
		}
		
		public void append(int ID) {
			IDs.add(ID);
		}
		
		public ClusterMemberID duplicate() {
			return new ClusterMemberID(this);
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof ClusterMemberID) {
				ClusterMemberID other = (ClusterMemberID)obj;
				if (IDs.size() != other.IDs.size()) return false;
				for (int i = 0; i < IDs.size(); i++)
					if (IDs.get(i) != other.IDs.get(i)) return false;
				return true;
			}
			return false;
		}
		
		public String toString() {
			String ret = "";
			for (Integer ID : IDs)
				ret = ID + "." + ret;
			return ret.substring(0, ret.length()-1);
		}

		@Override
		public int compareTo(ClusterMemberID o) {
			if (IDs.size() != o.IDs.size()) return 0;
			for (int i = IDs.size() -1; i >= 0; i--)
				if (IDs.get(i) < o.IDs.get(i)) return -1;
				else if (IDs.get(i) > o.IDs.get(i)) return 1;
			return 0;
		}
	}
}

