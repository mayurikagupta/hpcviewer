package edu.rice.cs.hpc.traceAnalysis.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Stack;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.ExperimentWithoutMetrics;
import edu.rice.cs.hpc.data.experiment.extdata.FileDB2;
import edu.rice.cs.hpc.data.experiment.extdata.IFileDB;
import edu.rice.cs.hpc.data.experiment.extdata.TraceAttribute;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.traceviewer.data.db.TraceDataByRank;
import edu.rice.cs.hpc.traceviewer.data.graph.CallPath;
import edu.rice.cs.hpc.traceviewer.data.version2.BaseData;
import edu.rice.cs.hpc.traceviewer.data.version3.FileDB3;

public class HPCToolkitTraceReader {
	final static private int MIN_TRACE_SIZE = TraceDataByRank.HeaderSzMin + TraceDataByRank.RecordSzMin * 2;
	final static public int RECORD_SIZE    = Constants.SIZEOF_LONG + Constants.SIZEOF_INT;
	
	PrintStream objPrint;
	PrintStream objError;
	
	private String traceFilePath;
	
    private ExperimentWithoutMetrics exp;
    private BaseData dataTrace;
    HashMap<Integer, CallPath> scopeMap;
    
    int maxDepth;
    long minTime;
    long frequency;
	
	public HPCToolkitTraceReader(File expFile, PrintStream objPrint, PrintStream objError) {
		this.objPrint = objPrint;
		this.objError = objError;
		
		exp = new ExperimentWithoutMetrics();
		try {
			// possible java.lang.OutOfMemoryError exception here
			exp.open(expFile, null);
			
			HPCToolkitTraceDataVisitor visitor = new HPCToolkitTraceDataVisitor();
			RootScope root = exp.getRootScope(RootScopeType.CallingContextTree);
			root.dfsVisitScopeTree(visitor);

			maxDepth   = visitor.getMaxDepth();
			scopeMap   = visitor.getMap();
		} catch (Exception e) {
			e.printStackTrace();
			objError.println("Error: unable to read the database file: " + 
					expFile.getAbsolutePath() + "\n" + e.getMessage());
			return;
		}

		IFileDB fileDB;
		
		try {
			final TraceAttribute trAttribute = exp.getTraceAttribute();		
			final int version = exp.getMajorVersion();
			
			minTime = trAttribute.dbTimeMin;
			
			if (version == 1 || version == 2)
			{	// original format
				fileDB = new FileDB2();
				traceFilePath = getTraceFile(exp.getDefaultDirectory().getAbsolutePath());
				fileDB.open(traceFilePath, trAttribute.dbHeaderSize, RECORD_SIZE);
				
			} else if (version == 3) 
			{
				// new format
				fileDB = new FileDB3();
				traceFilePath = exp.getDefaultDirectory() + File.separator + exp.getDbFilename(BaseExperiment.Db_File_Type.DB_TRACE);
				((FileDB3)fileDB).open(exp.getDefaultDirectory().getAbsolutePath());
			}
			else {
				objError.println("Trace data version is not unknown: " + version);
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			objError.println("Error: unable to parse the trace file: " + 
					traceFilePath + "\n" + e.getMessage());
			return;
		}
		
		dataTrace = new BaseData(fileDB);
	}
	
	/*********************
	 * get the absolute path of the trace file (experiment.mt).
	 * If the file doesn't exist, it is possible it is not merged yet 
	 *  (in this case we'll merge them automatically)
	 *********************/
	private String getTraceFile(String directory)
	{
		final String outputFile = directory
				+ File.separatorChar + "experiment.mt";

		File fileTrace = new File(outputFile);

		if (fileTrace.length() <= MIN_TRACE_SIZE)
			objError.println("Warning! Trace file "
					+ fileTrace.getName()
					+ " is too small: "
					+ fileTrace.length() + "bytes .");

		return fileTrace.getAbsolutePath();
	}
	
	public int getNumberOfRanks() {
		return dataTrace.getNumberOfRanks();
	}
	
	private int computeLCADepth(CallPath last, CallPath cur) {
		int depth = Math.min(last.getMaxDepth(), cur.getMaxDepth()) - 1;
		while (depth >= 0 && last.getScopeAt(depth).getCCTIndex() != cur.getScopeAt(depth).getCCTIndex()) depth--;
		
		return depth;
	}
	
	public TraceTree buildTraceTree(int rank) {
		if (rank >= dataTrace.getNumberOfRanks())
			return null;
		
		long minloc = dataTrace.getMinLoc(rank);
		long maxloc = dataTrace.getMaxLoc(rank);
		
		TraceTreeNode root = new TraceTreeNode(null, -1);
		
		Stack<TraceTreeNode> activeTraceStack = new Stack<TraceTreeNode>();
		activeTraceStack.add(root);
		try {
			long sampleNum = 0;
			
			// Get the first sample
			long begTime = dataTrace.getLong(minloc) - minTime;
			
			long lastTime = begTime;
			CallPath lastCP = scopeMap.get(dataTrace.getInt(minloc + Constants.SIZEOF_LONG));
			root.setStartTimeInclusive(lastTime);
			root.setStartTimeExclusive(lastTime);
			root.setStartSampleInclusive(sampleNum);
			
			for (int depth = 0; depth < lastCP.getMaxDepth(); depth++) {
				TraceTreeNode node = new TraceTreeNode(lastCP.getScopeAt(depth), depth);
				node.setStartTimeInclusive(lastTime);
				node.setStartTimeExclusive(lastTime);
				root.setStartSampleInclusive(sampleNum);
				activeTraceStack.peek().addChild(node);
				activeTraceStack.add(node);
			}
			
			// Get remaining samples
			for (long pos = minloc + RECORD_SIZE; pos <= maxloc; pos += RECORD_SIZE) {
				final long curTime = dataTrace.getLong(pos) - minTime;
				final CallPath curCP = scopeMap.get(dataTrace.getInt(pos + Constants.SIZEOF_LONG));
				final int lcaDepth = computeLCADepth(lastCP, curCP);
				
				if (curCP.getScopeAt(0).getName().equals("Partial Call Paths")) continue;
				
				while (activeTraceStack.size() > lcaDepth + 2) {
					TraceTreeNode inactiveNode = activeTraceStack.pop();
					inactiveNode.setEndTimeInclusive(lastTime);
					inactiveNode.setEndTimeExclusive(curTime);
					inactiveNode.setEndSampleInclusive(sampleNum);
				}
				
				sampleNum++;

				for (int depth = lcaDepth + 1; depth < curCP.getMaxDepth(); depth++) {
					TraceTreeNode node = new TraceTreeNode(curCP.getScopeAt(depth), depth);
					node.setStartTimeInclusive(curTime);
					node.setStartTimeExclusive(lastTime);
					node.setStartSampleInclusive(sampleNum);
					activeTraceStack.peek().addChild(node);
					activeTraceStack.add(node);
				}
				
				lastTime = curTime;
				lastCP = curCP;
			}
			
			// Handle leftovers at the end
			while (activeTraceStack.size() > 0) {
				TraceTreeNode inactiveNode = activeTraceStack.pop();
				inactiveNode.setEndTimeInclusive(lastTime);
				inactiveNode.setEndTimeExclusive(lastTime);
				inactiveNode.setEndSampleInclusive(sampleNum);
			}
			
			long endTime = lastTime;
			return new TraceTree(root, begTime, endTime, sampleNum+1);
			
		} catch (IOException e) {
			e.printStackTrace();
			objError.println("Error while reading trace file: " + 
					traceFilePath + " for rank #" + rank + "\n" + e.getMessage());
			return null;
		}
	}
	
	public boolean readRank(int rank){
		if (rank >= dataTrace.getNumberOfRanks())
			return false;
		
		long minloc = dataTrace.getMinLoc(rank);
		long maxloc = dataTrace.getMaxLoc(rank);
		
		try {
			for (long pos = minloc; pos <= maxloc; pos += RECORD_SIZE) {
				final long time = dataTrace.getLong(pos) - minTime;
				final int cpid = dataTrace.getInt(pos + Constants.SIZEOF_LONG);

				CallPath cp = scopeMap.get(cpid);
				if (cp == null) 
					objPrint.println("Rank " + rank + " at time " + time + ": no call path");
				else
					objPrint.println("Rank " + rank + " at time " + time + ": depth = " + + cp.getMaxDepth() + " " + cp.getDisplayNames());
			}
		} catch (IOException e) {
			e.printStackTrace();
			objError.println("Error while reading trace file: " + 
					traceFilePath + " for rank #" + rank + "\n" + e.getMessage());
			return false;
		}
		return true;
	}
}
