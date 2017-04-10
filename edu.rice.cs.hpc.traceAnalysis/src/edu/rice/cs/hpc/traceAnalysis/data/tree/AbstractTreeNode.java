package edu.rice.cs.hpc.traceAnalysis.data.tree;

abstract public class AbstractTreeNode {
	static protected int printDivisor = 1;
	
	final protected int ID;
	protected String name;
    protected int depth;

	public AbstractTreeNode(int ID, String name, int depth) {
		this.ID = ID;
		this.name = name;
		this.depth = depth;
	}
	
	public AbstractTreeNode(AbstractTreeNode other) {
		this.ID = other.ID;
		this.name = other.name;
		this.depth = other.depth;
	}
	
	public int getID() {
		return ID;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	abstract public boolean isLeaf();
	
	abstract public void clearChildren();
	
	public long getDuration() {
		return Math.max(getMinDuration(), 0);
	}
	
	abstract public long getMinDuration();
	
	abstract public long getMaxDuration();
	
	abstract public AbstractTreeNode duplicate();
	
	abstract public String print(int maxDepth, long durationCutoff);
}
