package edu.rice.cs.hpc.traceviewer.depth;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;

import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractDataController;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractProcessData;
import edu.rice.cs.hpc.traceviewer.data.db.DataPreparation;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.TimelineDataSet;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;

import edu.rice.cs.hpc.traceviewer.timeline.BaseTimelineThread;


/*************************************************
 * 
 * Timeline thread for depth view
 *
 *************************************************/
public class TimelineDepthThread 
	extends BaseTimelineThread
{

	/*****
	 * Thread initialization
	 *  
	 * @param data : global data
	 * @param canvas : depth view canvas
	 * @param scaleX : The scale in the x-direction of pixels to time 
	 * @param scaleY : The scale in the y-direction of max depth
	 * @param width  : the width
	 */
	public TimelineDepthThread(AbstractDataController data, 
			ImageTraceAttributes attributes,
			double scaleY, Queue<TimelineDataSet> queue, 
			AtomicInteger timelineDone, 
			boolean usingMidpoint, IProgressMonitor monitor)
	{
		super(data, attributes, scaleY, queue, timelineDone,  usingMidpoint, monitor);
	}


	@Override
	protected AbstractProcessData getNextTrace(AtomicInteger currentLine) {
		return stData.getNextDepthData(currentLine, attributes, monitor);
	}

	@Override
	protected boolean init(AbstractProcessData trace) {

		return true;
	}

	@Override
	protected void finalize() {
	}

	@Override
	protected DataPreparation getData(ColorTable colorTable,
			AbstractProcessData timeline, long timeBegin, int linenum, int height,
			double pixelLength, boolean midPoint) {

		return new DepthDataPreparation(stData.getColorTable(), 
				timeline, timeBegin,
				linenum, height, pixelLength, midPoint);
	}	
}
