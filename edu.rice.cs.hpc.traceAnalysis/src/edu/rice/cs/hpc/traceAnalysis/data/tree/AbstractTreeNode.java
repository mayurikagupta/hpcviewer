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
	
	final protected int ID;
	protected String name;
    protected int depth;
    
    protected int weight;
    
    protected TraceTimeStruct traceTime;
    
    /**
     * The CFGGraph node that contains the control flow graph for this node.
     */
    protected transient CFGGraph cfgGraph;
    /**
     * The CFGNode node that contains the RA for callsites or VMA for loops.
     */
    protected transient CFGNode addrNode;
    
    protected TreeNodeMetrics metrics;
    
    /**
     * Scores are sum up of difference scores of (weight * (weight-1) / 2) pairs of nodes.
     */
    //protected double inclusiveDiffScore = 0;
    //protected double exclusiveDiffScore = 0;

	public AbstractTreeNode(int ID, String name, int depth, CFGGraph cfgGraph, CFGNode addrNode) {
		this.ID = ID;
		this.name = name;
		this.depth = depth;
		this.weight = 1;
		this.traceTime = null;
		this.cfgGraph = cfgGraph;
		this.addrNode = addrNode;
		this.metrics = new TreeNodeMetrics();
	}
	
	protected AbstractTreeNode(AbstractTreeNode other) {
		this.ID = other.ID;
		this.name = other.name;
		this.depth = other.depth;
		this.weight = other.weight;
		//this.inclusiveDiffScore = other.inclusiveDiffScore;
		//this.exclusiveDiffScore = other.exclusiveDiffScore;
		this.traceTime = (other.traceTime == null ? null : other.traceTime.duplicate());
		this.cfgGraph = other.cfgGraph;
		this.addrNode = other.addrNode;
		this.metrics = other.metrics.duplicate();
	}
	
	public int getID() {
		return ID;
	}

	public String getName() {
		return name;
	}
	
	public int getWeight() {
		return weight;
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
	
	public TreeNodeMetrics getMetrics() {
		return metrics;
	}
	
	//public double getInclusiveDiffScore() {
	//	return inclusiveDiffScore;
	//}
	
	//public double getExclusiveDiffScore() {
	//	return exclusiveDiffScore;
	//}
	
	public void setName(String name){
		this.name = name;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	//public void setInclusiveDiffScore(double diffScore) {
	//	this.inclusiveDiffScore = diffScore;
	//}
	
	//public void setExclusiveDiffScore(double diffScore) {
	//	this.exclusiveDiffScore = diffScore;
	//}

	abstract public boolean isLeaf();
	
	abstract public void clearChildren();
	
	public void clearDiffScore() {
		this.getMetrics().setInclusiveDiffScore(0);
		this.getMetrics().setExclusiveDiffScore(0);
	}
	
	public void stretchDiffScore(double multiplier, double divisor) {
		this.getMetrics().setInclusiveDiffScore(getMetrics().getInclusiveDiffScore() * multiplier / divisor);
		this.getMetrics().setExclusiveDiffScore(getMetrics().getExclusiveDiffScore() * multiplier / divisor);
	}
	
	public long getDuration() {
		return (this.getMinDuration() + this.getMaxDuration()) / 2;
	}
	
	abstract public long getMinDuration();
	
	abstract public long getMaxDuration();
	
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
		double t = metrics.getInclusiveDiffScore() * 2 / (weight * (weight-1)) / printDivisor;
		ret += "  In-diff = " + Math.round(t);
		t = metrics.getExclusiveDiffScore() * 2 / (weight * (weight-1)) / printDivisor;
		ret += "  Ex-diff = " + Math.round(t);
		return ret;
	}
	
	protected String diffRatioString(double totalDiff) {
		String ret = "";
		double t = metrics.getInclusiveDiffScore() / totalDiff * 100;
		ret += "  In-diff = " + String.format("%.2f", t) + "%";
		t = metrics.getExclusiveDiffScore() / totalDiff * 100;
		ret += "  Ex-diff = " + String.format("%.2f", t) + "%";
		return ret;
	}
}
