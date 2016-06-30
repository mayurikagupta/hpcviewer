package edu.rice.cs.hpc.data.experiment.extdata;

import java.io.IOException;

public interface IBaseTraceData extends IBaseData{

	/*****
	 * retrieve a 64-bytes data for a given location
	 * @param position
	 * @return
	 * @throws IOException
	 */
	long getLong(long position) throws IOException;

	/********
	 * Retrieve a 32-bytes data for a given location
	 * @param position
	 * @return
	 * @throws IOException
	 */
	int getInt(long position) throws IOException;
	
	/********
	 * Get the size of the record
	 * @return
	 */
	int getRecordSize();

	/*******
	 * get the start offset (location) of a given rank
	 * @param rank
	 * @return
	 */
	long getMinLoc(int rank);

	/*******
	 * get the end offset (location) of a given rank
	 * @param rank
	 * @return
	 */
	long getMaxLoc(int rank);
}
