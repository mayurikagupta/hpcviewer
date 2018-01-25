package edu.rice.cs.hpc.traceviewer.data.controller;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;

import edu.rice.cs.hpc.common.util.ProcedureAliasMap;
import edu.rice.cs.hpc.data.experiment.ExperimentWithoutMetrics;
import edu.rice.cs.hpc.data.experiment.InvalExperimentException;
import edu.rice.cs.hpc.data.experiment.extdata.TraceAttribute;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;

import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.graph.CallPathColorTable;
import edu.rice.cs.hpc.traceviewer.data.graph.ProcedureColorTable;
import edu.rice.cs.hpc.traceviewer.data.graph.CallPath;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;


/*******************************************************************************************
 * 
 * Class to store global information concerning the database and the trace.
 * The class is designed to work for both local and remote database. Any references have to 
 * 	be addressed to the methods of this class instead of the derived class to enable
 *  transparency.
 * 
 * @author Original authors: Sinchan Banarjee, Michael France, Reed Lundrum and Philip Taffet
 * 
 * Modification:
 * - 2013 Philip: refactoring into three classes : abstract (this class), local and remote
 * - 2014.2.1 Laksono: refactoring to make it as simple as possible and avoid code redundancy
 *
 *******************************************************************************************/
public abstract class TraceDataController extends AbstractDataController
{
	/** The map between the nodes and the cpid's. */
	private HashMap<Integer, CallPath> scopeMap;
	
	// We probably want to get away from this. The for code that needs it should be
	// in one of the threads. It's here so that both local and remote can use
	// the same thread class yet get their information differently.
	//protected AtomicInteger lineNum;
	//AtomicInteger depthLineNum;

	final protected ExperimentWithoutMetrics exp;
	
	final protected TraceReportReader reader;

	/***
	 * Constructor to create a data based on File. This constructor is more suitable
	 * for local database
	 * 
	 * @param _window : SWT window
	 * @param expFile : experiment file (XML format)
	 */
	public TraceDataController(IWorkbenchWindow _window, File expFile) 
			throws InvalExperimentException, Exception 
	{	
		super(_window);
		
		// attributes initialization
		attributes = new ImageTraceAttributes();
		
		exp = new ExperimentWithoutMetrics();
		try {
			// possible java.lang.OutOfMemoryError exception here
			exp.open(expFile, new ProcedureAliasMap());
		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(_window.getShell(), "Error: unable to parse the database", 
					"Unable to read the file: " + expFile.getAbsolutePath() + "\n" +
					e.getMessage());
		}
		
		reader = new TraceReportReader(exp.getDefaultDirectory().getAbsolutePath());
		
		init(_window);
	}
	
	/*****
	 * Constructor to create a data based on input stream, which is convenient for remote database
	 * 
	 * @param _window : SWT window
	 * @param expStream : input stream
	 * @param Name : the name of the file on the remote server
	 * @throws InvalExperimentException 
	 *****/
	public TraceDataController(IWorkbenchWindow _window, InputStream expStream, String Name) 
			throws InvalExperimentException, Exception 
	{	
		super(_window);
		
		// attributes initialization
		attributes = new ImageTraceAttributes();
		
		exp = new ExperimentWithoutMetrics();

		// Without metrics, so param 3 is false
		exp.open(expStream, new ProcedureAliasMap(), Name);
		
		reader = new TraceReportReader(exp.getDefaultDirectory().getAbsolutePath());
		
		init(_window);
	}
	
	/******
	 * Initialize the object
	 * 
	 * @param _window
	 * @throws Exception 
	 ******/
	private void init(final IWorkbenchWindow window) 
			throws InvalExperimentException 
	{	
		final Display display = Display.getDefault();
		display.syncExec(new Runnable() {

			@Override
			public void run() {
				if (reader.isValid()) 
					colorTable = new CallPathColorTable(window);
				else
					colorTable = new ProcedureColorTable(window);
				
				// tree traversal to get the list of cpid, procedures and max depth
				TraceDataVisitor visitor = new TraceDataVisitor(window, colorTable);
				RootScope root = exp.getRootScope(RootScopeType.CallingContextTree);
				root.dfsVisitScopeTree(visitor);

				maxDepth   = visitor.getMaxDepth();
				scopeMap   = visitor.getMap();
		
				// initialize colors
				colorTable.setColorTable();
				reader.colorCallpaths((CallPathColorTable)colorTable);
				
				//lineNum 	 = new AtomicInteger(0);
				//depthLineNum = new AtomicInteger(0);
			}			
		});
		final TraceAttribute trAttribute = exp.getTraceAttribute();
		
		if (trAttribute == null) {
			throw new InvalExperimentException("Database does not contain traces: " + exp.getDefaultDirectory());
		}
		minBegTime = trAttribute.dbTimeMin;
		maxEndTime = trAttribute.dbTimeMax;
	}

	@Override
	public String getShortName() {
		return "trace";
	}

	/******
	 * get the depth trace of the current "selected" process
	 *  
	 * @return ProcessTimeline
	 */
	@Override
	public ProcessTimeline getCurrentDepthData() {
		int scaledDTProcess = computeScaledRank();
		return  (ProcessTimeline)ptlService.getProcessData(scaledDTProcess);
	}
	
	/***********************************************************************
	 * Gets the next available trace to be filled/painted from the DepthTimeView
	 * 
	 * @return The next trace.
	 **********************************************************************/
	@Override
	public ProcessTimeline getNextDepthData(AtomicInteger depthLineNum, 
			ImageTraceAttributes attributes, IProgressMonitor monitor) {
		
		ProcessTimeline depthTrace = getCurrentDepthData();
		if (depthTrace == null) {
			monitor.setCanceled(true);
			monitor.done(); // forcing to reset the title bar
			return null;
		}
		
		int currentDepthLineNum = depthLineNum.getAndIncrement();
		if (currentDepthLineNum < Math.min(attributes.numPixelsDepthV, maxDepth)) {
			
			// I can't get the data from the ProcessTimeline directly, so create
			// a ProcessTimeline with data=null and then copy the actual data to
			// it.
			ProcessTimeline toDonate = new ProcessTimeline(currentDepthLineNum,
					scopeMap, dataTrace, getCurrentlySelectedRank(), attributes.numPixelsH,
					attributes.getTimeInterval(), minBegTime
							+ attributes.getTimeBegin());

			toDonate.copyDataFrom(depthTrace);

			return toDonate;
		} else
			return null;
	}


	protected HashMap<Integer, CallPath> getScopeMap() {
		return scopeMap;
	}

	public int getHeaderSize() {
		final int headerSize = exp.getTraceAttribute().dbHeaderSize;
		return headerSize;
	}

	/*public void resetCounter() {
		lineNum.set(0);
	}
	
	public int getNumberOfLines() {
		return lineNum.get();
	}*/

/*	public void resetDepthCounter() {
		depthLineNum.set(0);
	}
	
	public int getNumberOfDepthLines() {
		return depthLineNum.get();
	}*/
	
	//see the note where this is called in FilterRanks
}