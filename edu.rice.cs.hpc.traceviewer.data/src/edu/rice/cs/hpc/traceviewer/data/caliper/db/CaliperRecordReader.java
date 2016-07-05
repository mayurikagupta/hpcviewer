package edu.rice.cs.hpc.traceviewer.data.caliper.db;

import java.io.IOException;
import java.io.RandomAccessFile;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;

/**
 * Reads the index and content of caliper data.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public class CaliperRecordReader {
	private RandomAccessFile index_file = null;
	private RandomAccessFile record_file = null;
	
	private long offset;
	private long numRecords;
	
	private final static int SIZE_HEADER = CaliperUtils.SIZE_INT;
	private final static int SIZE_THREAD_ENTRY = CaliperUtils.SIZE_LONG;
	private final static int SIZE_RECORD_ENTRY = CaliperUtils.SIZE_LONG + CaliperUtils.SIZE_LONG;
	
	public CaliperRecordReader(String cali_dir, int procID, int threadID) throws IOException {
		String indexFilename = cali_dir + CaliperUtils.RECORD_INDEX_FILE_PREFIX + procID;
		this.index_file = new RandomAccessFile(indexFilename, "r");
		
		String recordFilename = cali_dir + procID;
		this.record_file = new RandomAccessFile(recordFilename, "r");
	
		int numThread = index_file.readInt();
		
		long recordIndexPos = SIZE_HEADER + numThread * SIZE_THREAD_ENTRY;
		
		long threadInfoPos = SIZE_HEADER + threadID * SIZE_THREAD_ENTRY;
		index_file.seek(threadInfoPos);
		this.offset = recordIndexPos + index_file.readLong() * SIZE_RECORD_ENTRY;
		
		long nextOffset;
		if (threadID != numThread - 1) 
			nextOffset = recordIndexPos + index_file.readLong() * SIZE_RECORD_ENTRY;
		else
			nextOffset = index_file.getChannel().size();
		
		this.numRecords = (nextOffset - this.offset) / SIZE_RECORD_ENTRY;
	}
	
	public boolean isValid() {
		return (this.index_file != null) && (this.record_file != null);
	}
	
	public void close() throws IOException {
		index_file.close();
		record_file.close();
		
		index_file = null;
		record_file = null;
	}
	
	public String readRecord(long timestamp) throws IOException {
		long pos = this.seekRecord(timestamp);
		if (pos == CaliperUtils.ERROR) return null;
		
		record_file.seek(pos);
		return record_file.readLine();
	}
	
	private long seekRecord(long timestamp) throws IOException {
		long h = 0;
		long t = numRecords;
		
		if (t <= h) return CaliperUtils.ERROR;
		
		while (h < t - 1) {
			long mid = (h+t)/2;
			// get time stamp for record #mid
			index_file.seek(offset + mid * SIZE_RECORD_ENTRY);
			long midTimestamp = index_file.readLong();
			if (timestamp < midTimestamp) t = mid;
			else h = mid;
		}
		
		index_file.seek(offset + h * SIZE_RECORD_ENTRY);
		
		// Double check if record #h's timestamp is no greater than the provided timestamp.
		// If so, return the position of record #h in the record file.
		if (timestamp < index_file.readLong()) return CaliperUtils.ERROR;
		else return index_file.readLong();
	}
}
