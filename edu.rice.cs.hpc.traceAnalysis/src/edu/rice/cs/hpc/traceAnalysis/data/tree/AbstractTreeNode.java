package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGCall;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;
import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;

abstract public class AbstractTreeNode implements Serializable {
	private static final long serialVersionUID = 2487139341048860091L;

	static protected int printDivisor = 1;
	
	/*
	 * Basic attributes
	 */
	final protected int ID;
	protected String name;
    protected int depth;
    
    /*
     * Time attribute
     */
    protected TraceTimeStruct traceTime;
    
    /*
     * CFG related attributes
     */
    protected transient CFGGraph cfgGraph; // The CFGGraph node that contains the control flow graph for this node.
    protected transient CFGNode addrNode; // The CFGNode node that contains the RA for callsites or VMA for loops.
    
    /*
     * Representative attributes
     */
    protected int weight = 1; // The number of instances this node represents.
    
    // Difference scores across all instances represented by this node. 
    // While clustering, the values are the sum up of difference scores of (weight * (weight-1) / 2) pairs of nodes
    // After clustering, the values will be the average difference score among (weight * (weight-1) / 2) pairs of nodes
    protected double inclusiveDiffScore = 0;
    protected double exclusiveDiffScore = 0;
    
    protected long minDurationRep = 0; // Minimum duration of all instances represented by this node
    protected long maxDurationRep = 0; // Maximum duration of all instances represented by this node
    protected long totalDurationRep = 0; // Sum of duration of all instances represented by this node  //TODO may overflow
	
    protected long inclusiveImprovement = 0;
    protected long exclusiveImprovement = 0;

	public AbstractTreeNode(int ID, String name, int depth, CFGGraph cfgGraph, CFGNode addrNode) {
		this.ID = ID;
		this.name = name;
		this.depth = depth;
		
		this.traceTime = null;
		
		this.cfgGraph = cfgGraph;
		this.addrNode = addrNode;
	}
	
	protected AbstractTreeNode(AbstractTreeNode other) {
		this.ID = other.ID;
		this.name = other.name;
		this.depth = other.depth;

		this.traceTime = (other.traceTime == null ? null : other.traceTime.duplicate());
		
		this.cfgGraph = other.cfgGraph;
		this.addrNode = other.addrNode;
		
		this.weight = other.weight;
		this.inclusiveDiffScore = other.inclusiveDiffScore;
		this.exclusiveDiffScore = other.exclusiveDiffScore;
		this.minDurationRep = other.minDurationRep;
		this.maxDurationRep = other.maxDurationRep;
		this.totalDurationRep = other.totalDurationRep;
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
	

	public TraceTimeStruct getTraceTime(){
		return traceTime;
	}
	
	public void setTraceTime(TraceTimeStruct time) {
		this.traceTime = time;
	}
	
	public CFGGraph getCFGGraph() {
		return cfgGraph;
	}
	
	public CFGNode getAddrNode() {
		return addrNode;
	}

	
	public int getWeight() {
		return weight;
	}
	
	public void setWeight(int weight) {
		this.weight = weight;
	}

	public double getInclusiveDiffScore() {
		return inclusiveDiffScore;
	}

	public void setInclusiveDiffScore(double inclusiveDiffScore) {
		this.inclusiveDiffScore = inclusiveDiffScore;
	}

	public double getExclusiveDiffScore() {
		return exclusiveDiffScore;
	}

	public void setExclusiveDiffScore(double exclusiveDiffScore) {
		this.exclusiveDiffScore = exclusiveDiffScore;
	}
	
	public void clearDiffScore() {
		this.inclusiveDiffScore = 0;
		this.exclusiveDiffScore = 0;
	}
	
	public void stretchDiffScore(double multiplier, double divisor) {
		this.inclusiveDiffScore = this.inclusiveDiffScore * multiplier / divisor;
		this.exclusiveDiffScore = this.exclusiveDiffScore * multiplier / divisor;
	}
	
	public void initDurationRep() {
		this.minDurationRep = this.getDuration();
		this.maxDurationRep = this.getDuration();
		this.totalDurationRep = this.getDuration() * weight;
	}
	
	public long getMinDurationRep() {
		return minDurationRep;
	}

	public void setMinDurationRep(long minDurationRep) {
		this.minDurationRep = minDurationRep;
	}

	public long getMaxDurationRep() {
		return maxDurationRep;
	}

	public void setMaxDurationRep(long maxDurationRep) {
		this.maxDurationRep = maxDurationRep;
	}

	public long getTotalDurationRep() {
		return totalDurationRep;
	}

	public void setTotalDurationRep(long totalDurationRep) {
		this.totalDurationRep = totalDurationRep;
	}

	public long getInclusiveImprovement() {
		return inclusiveImprovement;
	}

	public void setInclusiveImprovement(long inclusiveImprovement) {
		this.inclusiveImprovement = inclusiveImprovement;
	}

	public long getExclusiveImprovement() {
		return exclusiveImprovement;
	}

	public void setExclusiveImprovement(long exclusiveImprovement) {
		this.exclusiveImprovement = exclusiveImprovement;
	}

	public long getDuration() {
		return (this.getMinDuration() + this.getMaxDuration()) / 2;
	}
	
	abstract public long getMinDuration();
	
	abstract public long getMaxDuration();
	
	abstract public boolean isLeaf();
	
	abstract public void clearChildren();
	
	abstract public AbstractTreeNode duplicate();
	
	abstract public AbstractTreeNode voidDuplicate();
	
	abstract public String toString(int maxDepth, long durationCutoff, int weight);
	
	abstract public String printLargeDiffNodes(int maxDepth, long durationCutoff, long totalDiff);
	
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(cfgGraph == null ? null : cfgGraph.toString());
		out.writeObject(addrNode == null ? null : addrNode.toString());
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		String str = (String) in.readObject();
		this.cfgGraph = (CFGGraph) rebuildCFGNode(str);
		
		str = (String) in.readObject();
		this.addrNode = rebuildCFGNode(str);
	}
	
	protected CFGNode rebuildCFGNode(String str) {
		if (str == null) return null;
			
		String[] split = str.split("_");
		assert(split[1].subSequence(0, 2).equals("0x"));
		long addr = Long.decode(split[1]);
		if (split[0].equals("loop"))
			return TraceAnalysisUtils.lookupCFGLoop(addr);
		else if (split[0].equals("func"))
			return TraceAnalysisUtils.lookupCFGFunc(addr);
		else
			return new CFGCall(addr);
	}
	
	protected String diffScoreString(int weight) {
		String ret = "";
		double t = this.inclusiveDiffScore * 2 / (weight * (weight-1)) / printDivisor;
		ret += "  In-diff = " + Math.round(t);
		t = this.exclusiveDiffScore * 2 / (weight * (weight-1)) / printDivisor;
		ret += "  Ex-diff = " + Math.round(t);
		return ret;
	}
	
	protected String diffRatioString(double totalDiff) {
		String ret = "";
		double t = this.inclusiveDiffScore / totalDiff * 100;
		ret += "  In-diff = " + String.format("%.2f", t) + "%";
		t = this.exclusiveDiffScore / totalDiff * 100;
		ret += "  Ex-diff = " + String.format("%.2f", t) + "%";
		return ret;
	}
}
