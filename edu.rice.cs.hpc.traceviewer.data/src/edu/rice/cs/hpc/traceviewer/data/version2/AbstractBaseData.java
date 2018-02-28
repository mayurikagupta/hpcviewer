package edu.rice.cs.hpc.traceviewer.data.version2;

import java.io.IOException;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseTraceData;
import edu.rice.cs.hpc.data.experiment.extdata.IFileDB;
import edu.rice.cs.hpc.data.util.Constants;

/*********************************************************
 * 
 * Abstract class to manage trace data. 
 * This class is the parent for all regular data and filtered data
 *
 *********************************************************/
public abstract class AbstractBaseData implements IBaseTraceData 
{
	final protected IFileDB baseDataFile;
	
	// timestamp and dLCA is encoded in one 64-bit integer.
	// timestamp is in lower 54 bits
	final private int TIMESTAMP_BITS = 54;
	final private long TIMESTAMP_MASK = (1L << TIMESTAMP_BITS) - 1L;
	
	// dLCA is in higher 10 bits
	final private int DLCA_BITS = 10;
	final private long DLCA_MASK = (1L << DLCA_BITS) - 1L;

	public AbstractBaseData(IFileDB baseDataFile){
		this.baseDataFile = baseDataFile;
	}
	
	private long getSamplePos(int rank, long sample) {
		return baseDataFile.getMinLoc(rank) + sample * baseDataFile.getRecordSize();
	}
	
	public long getNumSamples(int rank) {
		return baseDataFile.getNumSamples(rank);
	}
	
	public long getTimestamp(int rank, long sample) throws IOException {
		return baseDataFile.getLong(this.getSamplePos(rank, sample)) & TIMESTAMP_MASK;
	}
	
	public int getCpid(int rank, long sample) throws IOException {
		return baseDataFile.getInt(this.getSamplePos(rank, sample) + Constants.SIZEOF_LONG);
	}
	
	public boolean isLCARecorded() {
		return baseDataFile.isLCARecorded();
	}
	
	public int getdLCA(int rank, long sample) throws IOException {
		return (int) ((baseDataFile.getLong(this.getSamplePos(rank, sample)) >> TIMESTAMP_BITS) & DLCA_MASK);
	}
	
	public boolean isDataCentric() {
		return baseDataFile.isDataCentric();
	}
	
	public int getMetricID(int rank, long sample) throws IOException {
		return -1;
	}

	@Override
	public boolean isHybridRank() {
		return baseDataFile.getParallelismLevel() > 1;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.extdata.IBaseData#dispose()
	 */
	@Override
	public void dispose() {
		this.baseDataFile.dispose();
	}
	

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.extdata.IBaseData#getRecordSize()
	 */
	@Override
	public int getRecordSize() {
		return baseDataFile.getRecordSize();
	}

}
