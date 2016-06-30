package edu.rice.cs.hpc.traceviewer.main;


import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;

import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractDataController;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractProcessData;
import edu.rice.cs.hpc.traceviewer.data.abstraction.ProcessDataService;
import edu.rice.cs.hpc.traceviewer.data.db.DataPreparation;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.TimelineDataSet;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;

import edu.rice.cs.hpc.traceviewer.timeline.BaseTimelineThread;

public class TimelineThread 
	extends BaseTimelineThread
{
	final private int totalLines;
	final private ProcessDataService traceService;

	/**Stores whether or not the bounds have been changed*/
	private boolean changedBounds;
	
	/***********************************************************************************************************
	 * Creates a TimelineThread with SpaceTimeData _stData; the rest of the parameters are things for drawing
	 * @param changedBounds - whether or not the thread needs to go get the data for its ProcessTimelines.
	 ***********************************************************************************************************/
	public TimelineThread(AbstractDataController stData, ImageTraceAttributes attributes,
			ProcessDataService traceService,
			boolean _changedBounds, double _scaleY, Queue<TimelineDataSet> queue, 
			AtomicInteger currentLine, int totalLines, IProgressMonitor monitor)
	{
		super(stData, attributes, _scaleY, queue, currentLine, stData.isEnableMidpoint(), monitor);
		changedBounds = _changedBounds;		
		this.traceService = traceService;
		this.totalLines	  = totalLines;
	}
	
	
	@Override
	protected AbstractProcessData getNextTrace(AtomicInteger currentLine) {
		return stData.getNextData(currentLine, totalLines, attributes, changedBounds, monitor);
	}

	
	@Override
	protected boolean init(AbstractProcessData trace) throws IOException {
		//nextTrace.data is not empty if the data is from the server
		if(changedBounds)
		{
			if (trace.isEmpty()) {
				
				trace.readInData();
				if (!traceService.setProcessData(trace.line(), trace)) {
					// something wrong happens, perhaps data races ?
					monitor.setCanceled(true);
					monitor.done();
					return false;
				}
			}
			trace.shiftTimeBy(stData.getMinBegTime());
		}
		boolean res = (trace.size()>=2);
		return res;
	}

	@Override
	protected void finalize() {
	}

	@Override
	protected DataPreparation getData(ColorTable colorTable,
			AbstractProcessData timeline, long timeBegin, int linenum, int height,
			double pixelLength, boolean midPoint) {

		return new DetailDataPreparation(colorTable, timeline, 
				timeBegin, stData.getAttributes().getDepth(), height, pixelLength, midPoint);
	}

}