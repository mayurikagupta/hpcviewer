package edu.rice.cs.hpc.traceviewer.data.abstraction;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.data.experiment.extdata.IFilteredData;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;

public abstract class AbstractDataController {
	// Fields inited by this class.
	protected ImageTraceAttributes attributes;
	protected ProcessDataService ptlService;
	
	
	// Fields inited by child classes.
	/**
	 * The minimum beginning and maximum ending time stamp across all data.
	 */
	protected long maxEndTime, minBegTime;
	/** The maximum depth of any single CallStackSample in any trace. */
	protected int maxDepth;
	protected ColorTable colorTable;
	
	// Fields inited by later function calls.
	protected boolean enableMidpoint;
	protected IBaseData dataTrace = null;
	
	// nathan's data index variable
	// TODO: we need to remove this and delegate to the inherited class instead !
	private int currentDataIdx;
	
	
	public AbstractDataController(IWorkbenchWindow window) {
		// attributes initialization
		attributes = new ImageTraceAttributes();
		
		ISourceProviderService sourceProviderService = (ISourceProviderService) window.getService(ISourceProviderService.class);
		ptlService = (ProcessDataService) sourceProviderService.getSourceProvider(ProcessDataService.PROCESS_DATA_PROVIDER); 
	}

	/*************************************************************************
	 * Retrieve the name of the database. The name can be either the path of
	 * the directory, or the name of the profiled application, or both.
	 * <p>
	 * Ideally the name should be unique to distinguish with other databases. 
	 * 
	 * @return String: the name of the database
	 *************************************************************************/
	abstract public String getName() ;

	/***
	 * get the next process's data base on the current line
	 * 
	 * @param currentLine : atomic integer of current line
	 * @param changedBounds : boolean flag whether there's a change of boundary or not
	 * @return
	 */
	public abstract AbstractProcessData getNextData(AtomicInteger currentLine, int totalLines,
			ImageTraceAttributes attributes, boolean changedBounds, IProgressMonitor monitor);

	public abstract AbstractProcessData getCurrentDepthData();
	public abstract AbstractProcessData getNextDepthData(AtomicInteger depthLineNum, 
			ImageTraceAttributes attributes, IProgressMonitor monitor);
	
	public abstract void closeDB();

	public abstract IFilteredData createFilteredBaseData();

	public abstract void fillTracesWithData(boolean changedBounds, int numThreadsToLaunch)
			throws IOException;
	
	
	public void setDataIndex(int dataIndex) 
	{
		currentDataIdx = dataIndex;
	}
	
	public int getDataIndex()
	{
		return currentDataIdx;
	}
	
	public int getMaxDepth() 
	{
		return maxDepth;
	}
	
	public IBaseData getBaseData(){
		return dataTrace;
	}
	
	/******************************************************************************
	 * Returns number of processes (ProcessTimelines) held in this
	 * SpaceTimeData.
	 ******************************************************************************/
	public int getTotalTraceCount() {
		return dataTrace.getNumberOfRanks();
	}
	
	/******************************************************************************
	 * getter/setter trace attributes
	 * @return
	 ******************************************************************************/
	
	public int getPixelHorizontal() {
		return attributes.numPixelsH;
	}
	
	public ImageTraceAttributes getAttributes() {
		return attributes;
	}


	/*************************************************************************
	 * Returns width of the spaceTimeData: The width (the last time in the
	 * ProcessTimeline) of the longest ProcessTimeline.
	 ************************************************************************/
	public long getTimeWidth() {
		return maxEndTime - minBegTime;
	}

	public long getMaxEndTime() {
		return maxEndTime;
	}

	public long getMinBegTime() {
		return minBegTime;
	}

	public ColorTable getColorTable() {
		return colorTable;
	}

	public void dispose() {
		colorTable.dispose();
	}

	public void setEnableMidpoint(boolean enable) {
		this.enableMidpoint = enable;
	}

	public boolean isEnableMidpoint() {
		return enableMidpoint;
	}
	public IFilteredData getFilteredBaseData() {
		if (dataTrace instanceof IFilteredData)
			return (IFilteredData) dataTrace;
		return null;
	}
	
	/**
	 * changing the trace data, caller needs to make sure to refresh the views
	 * @param filteredBaseData
	 */
	public void setBaseData(IFilteredData filteredBaseData) {
		dataTrace = filteredBaseData;

		int endProcess = attributes.getProcessEnd();
		int begProcess = attributes.getProcessBegin();
		
		//Snap it back into the acceptable limits.
		if (endProcess > dataTrace.getNumberOfRanks())
			endProcess  = dataTrace.getNumberOfRanks();
		
		if (begProcess >= endProcess)
			begProcess = 0;
		
		attributes.setProcess(begProcess, endProcess);
	}

	public boolean isTimelineFilled() {
		return (ptlService != null && ptlService.isFilled());
	}
	
}
