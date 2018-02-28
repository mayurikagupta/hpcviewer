package edu.rice.cs.hpc.data.experiment.extdata;

import java.io.IOException;

public interface IBaseTraceData extends IBaseData {
	/********
	 * Get the size of the record
	 * @return
	 */
	int getRecordSize();
	
	public long getNumSamples(int rank);
	
	public long getTimestamp(int rank, long sample) throws IOException;
	
	public int getCpid(int rank, long sample) throws IOException;
	
	public boolean isLCARecorded();
	
	public int getdLCA(int rank, long sample) throws IOException;
	
	public boolean isDataCentric();
	
	public int getMetricID(int rank, long sample) throws IOException;
}
