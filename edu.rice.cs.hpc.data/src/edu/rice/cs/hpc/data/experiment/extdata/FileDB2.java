package edu.rice.cs.hpc.data.experiment.extdata;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.data.util.LargeByteBuffer;

/***************************************
 * 
 * An implementation of IFileDB for format 2.0
 *
 ***************************************/
public class FileDB2 implements IFileDB 
{
	//-----------------------------------------------------------
	// Global variables
	//-----------------------------------------------------------
	
	private int type = Constants.MULTI_PROCESSES | Constants.MULTI_THREADING; // default is hybrid
	
	private LargeByteBuffer masterBuff;
	
	private int numFiles = 0;
	private String valuesX[];
	private long offsets[];
	
	private int recordSz;
	private int headerSize;
	
	private RandomAccessFile file; 
	
	private boolean isLCARecorded = false;
	private boolean isDataCentric = false;

	@Override
	public void open(String filename, int headerSize) throws IOException 
	{
		if (filename != null) {
			// read header file
			readHeader(filename, headerSize);
		}
	}
	
	/***
	 * retrieve the array of process IDs
	 * 
	 * @return
	 */
	@Override
	public String []getRankLabels() {
		return valuesX;
	}
	
	@Override
	public int getNumberOfRanks() 
	{
		return this.numFiles;
	}
	
	@Override
	public long[] getOffsets() 
	{
		return this.offsets;
	}
	
	
	/***
	 * Read the header of the file and get info needed for further actions
	 * 
	 * @param f: array of files
	 * @throws IOException 
	 */
	private void readHeader(String filename, int headerSize)
			throws IOException {
		
		this.headerSize = headerSize;
		this.recordSz = Constants.SIZEOF_INT + Constants.SIZEOF_LONG;
		
		// Check original file headers to see if dLCA is recorded in trace.
		file = new RandomAccessFile(filename, "r");
		if (headerSize == 32) {
			DataInputStream in = new DataInputStream(new FileInputStream(filename));
			in.readInt(); // type
			in.readInt(); // numFiles
			
			in.readInt(); // proc_id
			in.readInt(); // thread_id
			
			long offset = in.readLong(); // offset for the first thread.
			in.skip(offset - 4 * Constants.SIZEOF_INT - Constants.SIZEOF_LONG); // go to the beginning of first thread
			
			// read 24-byte header magic, version, and endian
			byte[] header = new byte [24];
			in.read(header, 0, 24);
			
			// If it is a trace file.
			if ((new String(header)).substring(0, 18).equals("HPCRUN-trace______")) {
				long mask = in.readLong(); // read in hpctrace_hdr_flags_bitfield
				if ((mask & 1) != 0) { // isDataCentric
					this.recordSz += Constants.SIZEOF_INT;
					this.isDataCentric = true;
				}
				if ((mask & 2) != 0) { // isLCARecorded
					this.isLCARecorded = true;
				}
			}
			in.close();
		}
		
		final FileChannel f = file.getChannel();
		masterBuff = new LargeByteBuffer(f, headerSize, recordSz);

		this.type = masterBuff.getInt(0);
		this.numFiles = masterBuff.getInt(Constants.SIZEOF_INT);
		
		valuesX = new String[numFiles];
		offsets = new long[numFiles];
		
		long current_pos = Constants.SIZEOF_INT * 2;
		
		// get the procs and threads IDs
		for(int i=0; i<numFiles; i++) {

			final int proc_id = masterBuff.getInt(current_pos);
			current_pos += Constants.SIZEOF_INT;
			final int thread_id = masterBuff.getInt(current_pos);
			current_pos += Constants.SIZEOF_INT;
			
			offsets[i] = masterBuff.getLong(current_pos);
			current_pos += Constants.SIZEOF_LONG;
			
			//--------------------------------------------------------------------
			// adding list of x-axis 
			//--------------------------------------------------------------------			
			
			String x_val;
			if (this.isHybrid()) 
			{
				x_val = String.valueOf(proc_id) + "." + String.valueOf(thread_id);
			} else if (isMultiProcess()) 
			{
				x_val = String.valueOf(proc_id);					
			} else if (isMultiThreading()) 
			{
				x_val = String.valueOf(thread_id);
			} else {
				// temporary fix: if the application is neither hybrid nor multiproc nor multithreads,
				// we just print whatever the order of file name alphabetically
				// this is not the ideal solution, but we cannot trust the value of proc_id and thread_id
				x_val = String.valueOf(i);
			}
			valuesX[i] = x_val;
		}
	}

	@Override
	public int 	getParallelismLevel()
	{
		return (isHybrid()? 2 : 1);
	}

	/**
	 * Check if the application is a multi-processing program (like MPI)
	 * 
	 * @return true if this is the case
	 */
	public boolean isMultiProcess() {
		return (type & Constants.MULTI_PROCESSES) != 0;
	}
	
	/**
	 * Check if the application is a multi-threading program (OpenMP for instance)
	 * 
	 * @return
	 */
	public boolean isMultiThreading() {
		return (type & Constants.MULTI_THREADING) != 0;
	}
	
	/***
	 * Check if the application is a hybrid program (MPI+OpenMP)
	 * 
	 * @return
	 */
	public boolean isHybrid() {
		return (isMultiProcess() && isMultiThreading());
	}

	@Override
	public long getLong(long position) throws IOException {
		return masterBuff.getLong(position);
	}

	@Override
	public int getInt(long position) throws IOException {
		return masterBuff.getInt(position);
	}

	@Override
	public double 	getDouble(long position) throws IOException {
		return masterBuff.getDouble(position);
	}

	/***
	 * Disposing native resources
	 */
	public void dispose() {
		if (masterBuff != null)
			masterBuff.dispose();

		if (file != null) {
			try {
				// ------------------------------------------------------
				// need to close the file and its file channel
				// somehow this can free the memory
				// ------------------------------------------------------
				file.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public long getMinLoc(int rank) {
		final long offsets[] = getOffsets();
		long loc = -1;
		
		if (rank < offsets.length) {
			loc = offsets[rank] + headerSize;
		} else {
			throw new RuntimeException("File DB2: incorrect rank: " + rank +" (bigger than " + offsets.length+")");
		}
		return loc;
	}

	@Override
	public long getMaxLoc(int rank) {
		final long offsets[] = getOffsets();
		long maxloc = ( (rank+1<getNumberOfRanks())? 
				offsets[rank+1] : masterBuff.size()-1 )
				- recordSz;
		return maxloc;
	}

	@Override
	public boolean isLCARecorded() {
		return this.isLCARecorded;
	}

	@Override
	public boolean isDataCentric() {
		return this.isDataCentric;
	}

	@Override
	public long getNumSamples(int rank) {
		long maxloc = (rank+1<getNumberOfRanks())? 
				offsets[rank+1] : masterBuff.size();
		long minloc = offsets[rank] + headerSize;
		return (maxloc - minloc) / this.recordSz;
	}

	@Override
	public int getRecordSize() {
		return this.recordSz;
	}
}
