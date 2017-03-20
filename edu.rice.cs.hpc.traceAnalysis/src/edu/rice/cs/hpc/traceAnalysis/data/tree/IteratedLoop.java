package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.util.Vector;

public class IteratedLoop extends AbstractTraceNode {
	protected final RawLoop rawLoop;
	
	//protected Vector<AbstractTreeNode> iters = new Vector<AbstractTreeNode>();
	
	/**
	 * Time ranges for iteration. 
	 * If an iteration is a trace node, reference in this vector should be the same object as of the TraceTimeStruct in the iteration.
	 * If an iteration is a profile node, reference in this vector can be any object of TraceTimeStruct.
	 */
	//protected Vector<TraceTimeStruct> iterTime = new Vector<TraceTimeStruct>();

	
	public IteratedLoop(RawLoop rawLoop) {
		super(rawLoop.ID, rawLoop.name, rawLoop.depth);
		this.time = rawLoop.time;
		this.rawLoop = (RawLoop)rawLoop.duplicate();
	}
	
	public IteratedLoop(IteratedLoop other) {
		super(other);
		this.rawLoop = other.rawLoop;
		/*
		for (int i = 0; i < other.getNumOfIterations(); i++)
			if (other.getIteration(i) instanceof AbstractTraceNode) {
				AbstractTraceNode iter = (AbstractTraceNode) other.getIteration(i).duplicate();
				iters.add(iter);
				iterTime.add(iter.time);
			}
			else {
				iters.add(other.getIteration(i).duplicate());
				iterTime.add(other.getIterationTime(i).duplicate());
			}
			*/
	}
/*
	public void addIteration(Iteration iter) {
		iters.add(iter);
		iterTime.add(iter.getTime());
	}
	
	public AbstractTreeNode getIteration(int index) {
		return iters.get(index);
	}
	
	public void updateIteration(int index, AbstractTreeNode iter) {
		iters.set(index, iter);
	}
	
	public TraceTimeStruct getIterationTime(int index) {
		return iterTime.get(index);
	}
	
	public int getNumOfIterations() {
		return iters.size();
	}
	*/
	
	
	public AbstractTreeNode duplicate() {
		return new IteratedLoop(this);
	}
	
	
	public String print(int maxDepth, long durationCutoff) {
		/*
		String ret = "L";
		
		for (int i = 0; i < depth; i++) ret += "    ";

		ret += name + "(" + ID + ")";
		
		ret += " " + time.startTimeExclusive / 1000 + "/" +time.startTimeInclusive / 1000 + 
				" ~ " + time.endTimeInclusive / 1000 + "/" + + time.endTimeExclusive / 1000 + "\n";
		
		for (int i = 0; i < getNumOfIterations(); i++)
			ret += getIteration(i).print(maxDepth, durationCutoff);
		
		return ret;
		*/
		return "L" + super.print(maxDepth+1, durationCutoff).substring(1);
	}
}
