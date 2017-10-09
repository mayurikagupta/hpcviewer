package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.io.Serializable;
import java.util.Collections;
import java.util.Vector;


public class Cluster extends AbstractTreeNode {
	private static final long serialVersionUID = -1204550671079401928L;
	
	private AbstractTreeNode rep; // representative
	private Vector<ClusterMemberID> members = new Vector<ClusterMemberID>();
	
	public Cluster(AbstractTreeNode node, int id) {
		super(node);
		this.rep = node.duplicate();
		this.weight = rep.weight;
		if (this.rep instanceof AbstractTraceNode) {
			((AbstractTraceNode) this.rep).shiftTime(-((AbstractTraceNode) this.rep).getTime().getStartTimeExclusive());
		}
		this.members.add(new ClusterMemberID(id));
	}
	
	public Cluster(AbstractTreeNode node, Vector<ClusterMemberID> members) {
		super(node);
		this.rep = node.duplicate();
		this.weight = rep.weight;
		this.members.addAll(members);
	}
	
	protected Cluster(Cluster other) {
		super(other);
		this.rep = other.rep.duplicate();
		this.weight = rep.weight;
		for (ClusterMemberID otherMember : other.members)
			this.members.add(otherMember.duplicate());
	}
	
	public AbstractTreeNode getRep() {
		return rep;
	}

	@Override
	public int getWeight() {
		if (members.size() != rep.weight) 
			System.err.println("Error: cluster member size != weight " + " @ " + rep.ID + ", " + members.size() + " vs " + rep.weight);
		return rep.weight;
	}
	
	public int getMemberSize() {
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
	
	public String printLargeDiffNodes(int maxDepth, long durationCutoff, TraceTimeStruct ts, long totalDiff) {
		if (this.depth > maxDepth) return "";
		
		String ret = "  ";
		
		for (int i = 0; i < depth; i++) ret += "    ";
		
		Collections.sort(members);
		return ret + "Members in " + rep.getName() + " : " + members.toString() + "\n";
	}
	
	public String toString(int maxDepth, long durationCutoff, int weight) {
		String ret = "                ";
		
		for (int i = 0; i < depth; i++) ret += "    ";
		
		Collections.sort(members);
		return ret + "Members in " + rep.getName() + " : " + members.toString() + "\n" + rep.toString(maxDepth, durationCutoff, weight);
	}

	public void setDepth(int depth) {
		super.setDepth(depth);
		rep.setDepth(depth);
	}
	
	public void setWeight(int weight) {
		super.setWeight(weight);
		rep.setWeight(weight);
	}
	
	public boolean isLeaf() {
		return rep.isLeaf();
	}

	public void clearChildren() {
		assert(false);
	}
	
	public void clearDiffScore() {
		super.clearDiffScore();
		rep.clearDiffScore();
	}
	
	public void stretchDiffScore(double multiplier, double divisor) {
		super.stretchDiffScore(multiplier, divisor);
		rep.stretchDiffScore(multiplier, divisor);
	}

	public long getMinDuration() {
		return rep.getMinDuration();
	}

	public long getMaxDuration() {
		return rep.getMaxDuration();
	}

	public AbstractTreeNode duplicate() {
		return new Cluster(this);
	}
	
	public AbstractTreeNode voidDuplicate() {
		return new Cluster(this.rep.voidDuplicate(), new Vector<ClusterMemberID>());
	}
	
	public class ClusterMemberID implements Comparable<ClusterMemberID>, Serializable {
		private static final long serialVersionUID = -8225446671312668849L;
		
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
		
		public void remove() {
			IDs.remove(IDs.size()-1);
		}
		
		public ClusterMemberID duplicate() {
			return new ClusterMemberID(this);
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof ClusterMemberID) {
				ClusterMemberID other = (ClusterMemberID)obj;
				if (IDs.size() != other.IDs.size()) return false;
				for (int i = 0; i < IDs.size(); i++)
					if (!IDs.get(i).equals(other.IDs.get(i))) return false;
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
		
		@Override
		public int hashCode() {
			return toString().hashCode();
		}
	}

}

