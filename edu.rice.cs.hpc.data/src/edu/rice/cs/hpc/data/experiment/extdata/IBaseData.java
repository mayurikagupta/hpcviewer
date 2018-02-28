package edu.rice.cs.hpc.data.experiment.extdata;

/*************************************************************************
 * 
 * Interface for managing the access to "external" data
 * Note: External here means outside xml file
 *
 *************************************************************************/
public interface IBaseData 
{	
	/***
	 * retrieve the list of rank names ( usual format: process.thread )
	 * @return
	 */
	public String []getListOfRanks();
	
	/****
	 * retrieve the number of ranks 
	 * @return
	 */
	public int getNumberOfRanks();
	
	/**
	 * Get the index of the first included rank. Provided to give a
	 * window through the filtering abstraction
	 */
	public int getFirstIncluded();
	
	/***
	 * Get the last inclusive rank
	 * @return int rank
	 */
	public int getLastIncluded();

	/** Is every rank included between the first and the last as provided above?*/
	public boolean isDenseBetweenFirstAndLast();
	
	/** Return true if the application is a hybrid app (such as MPI+OpenMP). 
	 *  False otherwise
	 *  @return boolean **/
	public boolean isHybridRank();
	/****
	 * Disposing native resources, to be called by the caller
	 */
	public void dispose();
}
