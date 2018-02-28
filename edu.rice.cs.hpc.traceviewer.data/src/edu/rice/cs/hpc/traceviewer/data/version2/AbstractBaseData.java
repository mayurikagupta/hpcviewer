package edu.rice.cs.hpc.traceviewer.data.version2;

import java.io.IOException;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
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
		return baseDataFile.getLong(this.getSamplePos(rank, sample));
	}
	
	public int getCpid(int rank, long sample) throws IOException {
		return baseDataFile.getInt(this.getSamplePos(rank, sample) + Constants.SIZEOF_LONG);
	}
	
	public boolean isLCARecorded() {
		return baseDataFile.isLCARecorded();
	}
	
	public int getdLCA(int rank, long sample) throws IOException {
		return baseDataFile.getInt(this.getSamplePos(rank, sample) + Constants.SIZEOF_LONG + Constants.SIZEOF_INT);
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
