package edu.rice.cs.hpc.traceAnalysis.data.tree;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ShadowTraceTree extends AbstractTreeNode {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4352918726216186961L;

	private boolean clearedDiffScore = false;
	
	public ShadowTraceTree(String filename) {
		super(0, filename, 0);
	}
	
	public RootTrace getRootTrace() {
		try {
			FileInputStream fileIn = new FileInputStream(this.name);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			TraceTree tree = (TraceTree) in.readObject();
			in.close();
			fileIn.close();
			tree.root.setDepth(this.depth);
			if (clearedDiffScore) tree.root.clearDiffScore();
			return tree.root;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException c) {
	         c.printStackTrace();
	        return null;
	    }
	}
	
	@Override
	public void clearDiffScore() {
		this.clearedDiffScore = true;
	}
	
	@Override
	public boolean isLeaf() {
		return getRootTrace().isLeaf();
	}

	@Override
	public void clearChildren() {
	}

	@Override
	public long getMinDuration() {
		return getRootTrace().getMinDuration();
	}

	@Override
	public long getMaxDuration() {
		return getRootTrace().getMaxDuration();
	}

	@Override
	public AbstractTreeNode duplicate() {
		return this;
	}

	@Override
	public AbstractTreeNode voidDuplicate() {
		return this;
	}

	@Override
	public String toString(int maxDepth, long durationCutoff, int weight) {
		return getRootTrace().toString(maxDepth, durationCutoff, weight);
	}

	@Override
	public String printLargeDiffNodes(int maxDepth, long durationCutoff,
			TraceTimeStruct ts, long totalDiff) {
		return getRootTrace().printLargeDiffNodes(maxDepth, durationCutoff, ts, totalDiff);
	}
}
