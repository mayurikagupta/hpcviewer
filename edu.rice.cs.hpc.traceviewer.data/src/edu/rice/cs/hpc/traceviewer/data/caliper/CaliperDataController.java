package edu.rice.cs.hpc.traceviewer.data.caliper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.extdata.IFilteredData;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractDataController;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractProcessData;
import edu.rice.cs.hpc.traceviewer.data.caliper.db.CaliperDataSummary;
import edu.rice.cs.hpc.traceviewer.data.caliper.db.IBaseCaliperData;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperStackFrame;
import edu.rice.cs.hpc.traceviewer.data.controller.TraceDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;

public class CaliperDataController extends AbstractDataController {
	
	private final boolean isOpen;

	public CaliperDataController(IWorkbenchWindow window, TraceDataController traceController,
			String databaseDir) {
		super(window);
		
		// Use the same attribute for synchronization.
		this.attributes = traceController.getAttributes();

		this.minBegTime = traceController.getMinBegTime();
		this.maxEndTime = traceController.getMaxEndTime();
		
		if (databaseDir.charAt(databaseDir.length()-1) != File.separatorChar)
			databaseDir = databaseDir + File.separatorChar;
		CaliperDataSummary summary = new CaliperDataSummary(databaseDir + CaliperUtils.CALIPER_DIR);
		
		this.isOpen = summary.isCaliperDataOpen();

		if (this.isOpen) {
			this.maxDepth = summary.getMaxDepth();
			this.dataTrace = summary;		
	
			this.colorTable = new CaliperColorTable(window);
			for (CaliperStackFrame frame : summary.getStackFrames())
				colorTable.addName(frame.getDisplayName());
			this.colorTable.setColorTable();
		}
	}

	public boolean isCaliperOpen() {
		return isOpen;
	}
	
	@Override
	public String getName() {
		return ((IBaseCaliperData)dataTrace).getCaliperDir();
	}

	@Override
	public AbstractProcessData getNextData(AtomicInteger currentLine,
			int totalLines, ImageTraceAttributes attributes,
			boolean changedBounds, IProgressMonitor monitor) {
		ProcessCaliperData data = null;
		
		int currentLineNum = currentLine.getAndIncrement();
		if (currentLineNum < totalLines) {
			if (ptlService.getNumProcessData() == 0)
				ptlService.setProcessData(new ProcessCaliperData[totalLines]);
			
			if (changedBounds) {
				ProcessCaliperData currentData = new ProcessCaliperData(currentLineNum, 
						(IBaseCaliperData)dataTrace, lineToPaint(currentLineNum, attributes), 
						minBegTime + attributes.getTimeBegin(), attributes.getTimeInterval(),
						attributes.numPixelsH);
				
				if (ptlService.setProcessData(currentLineNum, currentData)) {
					data = currentData;
				} else {
					monitor.setCanceled(true);
					monitor.done();
				}
			} else {
				data = (ProcessCaliperData)ptlService.getProcessData(currentLineNum);
			}
		}
		
		return data;
	}

	@Override
	public ProcessCaliperData getCurrentDepthData() {
		int scaledDTProcess = computeScaledRank();
		return  (ProcessCaliperData) ptlService.getProcessData(scaledDTProcess);
	}

	@Override
	public ProcessCaliperData getNextDepthData(AtomicInteger depthLineNum,
			ImageTraceAttributes attributes, IProgressMonitor monitor) {
		
		ProcessCaliperData currentDepthData = getCurrentDepthData();
		if (currentDepthData == null) {
			monitor.setCanceled(true);
			monitor.done(); // forcing to reset the title bar
			return null;
		}
		
		int currentDepthLineNum = depthLineNum.getAndIncrement();	
		if (currentDepthLineNum < Math.min(attributes.numPixelsDepthV, maxDepth)) {
			ProcessCaliperData nextDepthData = new ProcessCaliperData(currentDepthLineNum, 
					(IBaseCaliperData)dataTrace, getCurrentlySelectedRank(), 
					minBegTime + attributes.getTimeBegin(), attributes.getTimeInterval(),
					attributes.numPixelsH);
			
			nextDepthData.copyDataFrom(currentDepthData);

			return nextDepthData; //toDonate;
		} else
			return null;
	}

	@Override
	public void closeDB() {
		dataTrace.dispose();
	}

	@Override
	public IFilteredData createFilteredBaseData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void fillTracesWithData(boolean changedBounds, int numThreadsToLaunch)
			throws IOException {
		// TODO Auto-generated method stub
		
	}

}
