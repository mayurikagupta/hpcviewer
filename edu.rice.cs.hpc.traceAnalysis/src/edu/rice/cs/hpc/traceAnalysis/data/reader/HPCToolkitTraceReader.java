package edu.rice.cs.hpc.traceAnalysis.data.reader;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.ExperimentWithoutMetrics;
import edu.rice.cs.hpc.data.experiment.extdata.FileDB2;
import edu.rice.cs.hpc.data.experiment.extdata.IFileDB;
import edu.rice.cs.hpc.data.experiment.extdata.TraceAttribute;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGCall;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGGraph;
import edu.rice.cs.hpc.traceAnalysis.data.cfg.CFGNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTraceNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.FunctionTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.RawLoopTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.RootTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTree;
import edu.rice.cs.hpc.traceAnalysis.utils.TraceAnalysisUtils;
import edu.rice.cs.hpc.traceviewer.data.db.TraceDataByRank;
import edu.rice.cs.hpc.traceviewer.data.version2.BaseData;
import edu.rice.cs.hpc.traceviewer.data.version3.FileDB3;

public class HPCToolkitTraceReader {
	final static private int MIN_TRACE_SIZE = TraceDataByRank.HeaderSzMin + TraceDataByRank.RecordSzMin * 2;
	final static public int RECORD_SIZE    = Constants.SIZEOF_LONG + Constants.SIZEOF_INT;
	
	private PrintStream objPrint;
	private PrintStream objError;
	
	private String traceFilePath;
	
    private ExperimentWithoutMetrics exp;
    private BaseData dataTrace;
    private HashMap<Integer, LineScope> scopeMap;
    
    //private int maxDepth;
    private long minTime;
    private long maxTime;
    //private long frequency;
	
    private CFGReader cfgReader;
    
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
			maxTime = trAttribute.dbTimeMax;
			
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
		
		cfgReader = new CFGReader(this.getCFGFile());
		if (!cfgReader.read(objError)) {
			objError.println("Error while processing CFG graphviz file.");
			return;
		}
		
		TraceAnalysisUtils.setCFGFuncMap(cfgReader.CFGFuncMap);
		TraceAnalysisUtils.setCFGLoopMap(cfgReader.CFGLoopMap);
		
		dataTrace = new BaseData(fileDB);
	}
	
	public String getCFGFile() {
		final String outputFile = exp.getDefaultDirectory()
				+ File.separator + "cfg.dot";
		return outputFile;
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
	
	public long getDurantion() {
		return maxTime - minTime;
	}
	
	private int computeLCADepth(CallPathWithLoop last, CallPathWithLoop cur) {
		int depth = Math.min(last.getMaxDepth(), cur.getMaxDepth()) - 1;
		while (depth >= 0 && last.getScopeAt(depth).getCCTIndex() != cur.getScopeAt(depth).getCCTIndex()) depth--;
		
		return depth;
	}
	
	private CFGCall findCFGCall(CFGGraph parentCFGNode, long ra) {
		if (parentCFGNode == null || !parentCFGNode.valid) return null;
		for (CFGNode node : parentCFGNode.nodes)
			if (node instanceof CFGCall && node.vma == ra)
				return (CFGCall) node;
		return null;
	}
	
	public TraceTree buildTraceTree(int rank) {
		if (rank >= dataTrace.getNumberOfRanks())
			return null;
		
		long minloc = dataTrace.getMinLoc(rank);
		long maxloc = dataTrace.getMaxLoc(rank);
		
		RootTrace root = new RootTrace("Root for proc #" + rank);
		
		Stack<AbstractTraceNode> activeTraceStack = new Stack<AbstractTraceNode>();
		activeTraceStack.add(root);
		try {
			long sampleNum = 0;
			
			// Get the first sample
			long begTime = dataTrace.getLong(minloc) - minTime;
			
			long lastTime = begTime;
			CallPathWithLoop lastCP = new CallPathWithLoop(scopeMap.get(dataTrace.getInt(minloc + Constants.SIZEOF_LONG)));
			root.getTraceTime().setStartTimeInclusive(lastTime);
			root.getTraceTime().setStartTimeExclusive(lastTime-1);
			
			for (int depth = 0; depth < lastCP.getMaxDepth(); depth++) {
				Scope scope = lastCP.getScopeAt(depth);
				AbstractTraceNode node = null;
				
				if (scope instanceof LoopScope) {
					node = new RawLoopTrace(scope.getCCTIndex(), scope.getName(), depth+1, 
							cfgReader.CFGLoopMap.get(((LoopScope)scope).getVMA()));
					if (node.getCFGGraph() == null)
						System.err.println("Loop_0x" + Long.toHexString(((LoopScope)scope).getVMA()) + 
								" not found in cfg.dot");
				}
				
				if (scope instanceof CallSiteScope) {
					long ra = ((CallSiteScope)scope).getRA();
					if (ra != 0) {
						node = new FunctionTrace(scope.getCCTIndex(), scope.getName(), depth+1, 
							cfgReader.CFGFuncMap.get(((CallSiteScope)scope).getVMA()), 
							findCFGCall(activeTraceStack.peek().getCFGGraph(), ra));
						if (activeTraceStack.peek().getCFGGraph() != null && node.getAddrNode() == null)
							System.err.println("RA " + Long.toHexString(ra) + " not found under " + activeTraceStack.peek().getCFGGraph());
					}
				}
				
				if (node == null) {
//					System.err.println("Skipped: " + scope.getName());
					continue;
				}

				node.getTraceTime().setStartTimeInclusive(lastTime);
				node.getTraceTime().setStartTimeExclusive(lastTime);
				
				activeTraceStack.peek().addChild(node);
				activeTraceStack.add(node);
			}
			
			// Get remaining samples
			for (long pos = minloc + RECORD_SIZE; pos <= maxloc; pos += RECORD_SIZE) {
				final long curTime = dataTrace.getLong(pos) - minTime;
				final CallPathWithLoop curCP = new CallPathWithLoop(scopeMap.get(dataTrace.getInt(pos + Constants.SIZEOF_LONG)));
				final int lcaDepth = computeLCADepth(lastCP, curCP);
				
				if (curCP.getScopeAt(0).getName().equals("Partial Call Paths")) continue;
				
				while (activeTraceStack.size() > lcaDepth + 2) {
					AbstractTraceNode inactiveNode = activeTraceStack.pop();
					inactiveNode.getTraceTime().setEndTimeInclusive(lastTime);
					inactiveNode.getTraceTime().setEndTimeExclusive(curTime);
				}
				
				sampleNum++;

				for (int depth = lcaDepth + 1; depth < curCP.getMaxDepth(); depth++) {
					Scope scope = curCP.getScopeAt(depth);
					AbstractTraceNode node = null;
					
					if (scope instanceof LoopScope) {
						node = new RawLoopTrace(scope.getCCTIndex(), scope.getName(), depth+1, 
								cfgReader.CFGLoopMap.get(((LoopScope)scope).getVMA()));
						if (node.getCFGGraph() == null)
							System.err.println("Loop_0x" + Long.toHexString(((LoopScope)scope).getVMA()) + 
									" not found in cfg.dot");
					}
					
					if (scope instanceof CallSiteScope) {
						long ra = ((CallSiteScope)scope).getRA();
						if (ra != 0) {
							node = new FunctionTrace(scope.getCCTIndex(), scope.getName(), depth+1, 
								cfgReader.CFGFuncMap.get(((CallSiteScope)scope).getVMA()), 
								findCFGCall(activeTraceStack.peek().getCFGGraph(), ra));
							if (activeTraceStack.peek().getCFGGraph() != null && node.getAddrNode() == null)
								System.err.println("RA " + Long.toHexString(ra) + " not found under " + activeTraceStack.peek().getCFGGraph());
						}
					}
					
					if (node == null) {
//						System.err.println("Skipped: " + scope.getName());
						continue;
					}
					
					node.getTraceTime().setStartTimeInclusive(curTime);
					node.getTraceTime().setStartTimeExclusive(lastTime);
					
					activeTraceStack.peek().addChild(node);
					activeTraceStack.add(node);
				}
				
				lastTime = curTime;
				lastCP = curCP;
			}
			
			// Handle leftovers at the end
			while (activeTraceStack.size() > 0) {
				AbstractTraceNode inactiveNode = activeTraceStack.pop();
				inactiveNode.getTraceTime().setEndTimeInclusive(lastTime);
				inactiveNode.getTraceTime().setEndTimeExclusive(lastTime+1);
			}
			
			long endTime = lastTime;
			root.initDurationRep();
			return new TraceTree(root, begTime, endTime, sampleNum+1);
			
		} catch (IOException e) {
			e.printStackTrace();
			objError.println("Error while reading trace file: " + 
					traceFilePath + " for rank #" + rank + "\n" + e.getMessage());
			return null;
		}
	}
	
	public boolean printRank(int rank){
		if (rank >= dataTrace.getNumberOfRanks())
			return false;
		
		long minloc = dataTrace.getMinLoc(rank);
		long maxloc = dataTrace.getMaxLoc(rank);
		
		try {
			for (long pos = minloc; pos <= maxloc; pos += RECORD_SIZE) {
				final long time = dataTrace.getLong(pos) - minTime;
				final int cpid = dataTrace.getInt(pos + Constants.SIZEOF_LONG);

				LineScope scope = scopeMap.get(cpid);
				if (scope == null) 
					objPrint.println("Rank " + rank + " at time " + time + ": no call path");
				else {
					CallPathWithLoop cp = new CallPathWithLoop(scope);
					objPrint.println("Rank " + rank + " at time " + time + ": depth = " + + cp.getMaxDepth() + " " + cp.getDisplayNames());
				}
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

class CallPathWithLoop {
	Scope[] scopes;
	
	CallPathWithLoop(LineScope leafscope) {
		LinkedList<Scope> stack = new LinkedList<Scope>();
		Scope scope = leafscope;
		while ((scope != null) && !(scope instanceof RootScope)) {
			if ((scope instanceof CallSiteScope) || (scope instanceof LoopScope))
				stack.push(scope);
			scope = scope.getParentScope();
		}
		scopes = stack.toArray(new Scope[0]);
	}
	
	int getMaxDepth() {
		return scopes.length;
	}
	
	Scope getScopeAt(int depth) {
		return scopes[depth];
	}
	
	public String[] getDisplayNames()
	{
		final String[] names = new String[scopes.length];
		for (int i = 0; i < scopes.length; i++)
			names[i] = scopes[i].getName();
		return names;
	}
}