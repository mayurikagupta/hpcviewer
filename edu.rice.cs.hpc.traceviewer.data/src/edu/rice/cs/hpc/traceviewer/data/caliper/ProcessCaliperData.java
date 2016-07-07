package edu.rice.cs.hpc.traceviewer.data.caliper;

import java.io.IOException;
import java.util.Vector;

import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractProcessData;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractStack;
import edu.rice.cs.hpc.traceviewer.data.caliper.db.CaliperRecord;
import edu.rice.cs.hpc.traceviewer.data.caliper.db.CaliperRecordReader;
import edu.rice.cs.hpc.traceviewer.data.caliper.db.IBaseCaliperData;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperIteration;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperStackFrame;

/**
 * Provides the data of a caliper rank.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public class ProcessCaliperData extends AbstractProcessData {
	private final IBaseCaliperData caliperData;
	
	/** This rank's proc ID and thread ID*/
	private final int procID;
	private final int threadID;
	
	/** The initial time in view. */
	private final long startingTime;

	/** The range of time in view. */
	private final long timeRange;

	/** The number of pixels available in the view. */
	private final int numPixelH;
	
	/** All sampled records. */
	private Vector<CaliperRecord> records;
	
	/** Shifted time. */
	private long shiftTime = 0;
	
	public ProcessCaliperData(int lineNum, IBaseCaliperData caliperData, int rankID, 
			long startingTime, long timeRange, int numPixelH) {
		super(lineNum);
		this.caliperData = caliperData;
		this.procID = caliperData.getProcID(rankID);
		this.threadID = caliperData.getThreadID(rankID);
		
		this.startingTime = startingTime;
		this.timeRange = timeRange;
		
		this.numPixelH = numPixelH;
		
		this.records = new Vector<CaliperRecord>(numPixelH);
	}

	@Override
	public void readInData() throws IOException {
		CaliperRecordReader reader = new CaliperRecordReader(
				caliperData.getCaliperDir(), procID, threadID);
		
		if (reader.isValid()) {
			for (int i = 0; i < numPixelH; i++) {
				long timestamp = this.startingTime + i * timeRange / numPixelH;
				CaliperStack stack = parseRecord(reader.readRecord(timestamp));
				if (stack != null)
					records.add(new CaliperRecord(timestamp, stack));
			}
			reader.close();
		}
	}
	
	private CaliperStack parseRecord(String record) {
		if (record == null) return null;
		
		String stack = record.substring(0, record.indexOf(CaliperUtils.SECTION_SEPARATOR));
		String[] frameNames = stack.split(CaliperUtils.FRAME_SEPARATOR);
		
		Vector<CaliperStackFrame> frames = new Vector<CaliperStackFrame>(frameNames.length + 1);
		frames.add(CaliperUtils.CALIPER_ROOT_FRAME);
		
		for (int i = 0; i < frameNames.length; i++) {
			if (frameNames[i].length() == 0) continue;
			if (frameNames[i].charAt(0) == CaliperUtils.PHASE_INDICATOR) {
				String phaseName = frameNames[i].substring(1);
				frames.add(caliperData.getPhase(phaseName));
			}
			if (frameNames[i].charAt(0) == CaliperUtils.ITERATION_INDICATOR) {
				int pos = frameNames[i].indexOf(CaliperUtils.ITERATION_SEPARATOR);
				long iteration = Long.parseLong(frameNames[i].substring(1, pos));
				String loopName = frameNames[i].substring(pos + 1);
				
				frames.add(new CaliperIteration(iteration, caliperData.getLoop(loopName)));
			}
		}
		
		return new CaliperStack(frames);
	}

	@Override
	public long getTime(int sample) {
		if (sample < 0 || sample >= records.size()) return 0;
		return records.get(sample).timestamp - shiftTime;
	}

	@Override
	public AbstractStack getStack(int sample) {
		if (sample < 0 || sample >= records.size()) return null;
		return records.get(sample).stack;
	}

	@Override
	public void shiftTimeBy(long lowestStartingTime) {
		this.shiftTime += lowestStartingTime;
	}

	@Override
	public int size() {
		return records.size();
	}

	@Override
	public boolean isEmpty() {
		return records.isEmpty();
	}

	private long getTimeMidPoint(int left, int right) {
		return (records.get(left).timestamp + records.get(right).timestamp) / 2;
	}
	
	@Override
	public int findClosestSample(long time, boolean usingMidpoint) {
		if (records.size()==0)
			return 0;
		
		time += shiftTime;

		int low = 0;
		int high = records.size() - 1;
		
		long timeMin = records.get(low).timestamp;
		long timeMax = records.get(high).timestamp;
		
		// do not search the sample if the time is out of range
		if (time < timeMin || time > timeMax) return -1;
		
		int mid = (low + high) / 2;
		
		while(low != mid)
		{
			final long time_current = (usingMidpoint ? getTimeMidPoint(mid,mid+1) : 
				records.get(mid).timestamp);
			
			if (time > time_current)
				low = mid;
			else
				high = mid;
			mid = ( low + high ) / 2;
			
		}
		if (usingMidpoint)
		{
			if (time >= getTimeMidPoint(low,low+1))
				return low+1;
			else
				return low;
		} 
		else 
		{
			// without using midpoint, we adopt the leftmost sample approach.
			// this means whoever on the left side, it will be the painted
			return low;
		}
	}

	public void copyDataFrom(ProcessCaliperData other) {
		this.records = other.records;
		this.shiftTime = other.shiftTime;
	}
}
