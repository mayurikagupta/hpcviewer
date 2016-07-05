package edu.rice.cs.hpc.traceviewer.data.caliper.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Vector;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperLoop;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperPhase;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperStackFrame;


/**
 * Summary of caliper data.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Class created.
 */
public class CaliperDataSummary implements IBaseCaliperData {
	private String caliperDir;
	
	private boolean isOpen;
	
	private int numProc;
	private int numThread;
	
	String[] rankNames = null;
	RankInfo[] rankInfos = null;
	
	private int numPhase;
	Hashtable<String, CaliperPhase> phases = null;
	
	private int numLoop;
	Hashtable<String, CaliperLoop> loops = null;

	class RankInfo {
		final int procID;
		final int threadID;
		
		RankInfo(int procID, int threadID) {
			this.procID = procID;
			this.threadID = threadID;
		}
	}

	public CaliperDataSummary (String caliperDir) {
		this.caliperDir = caliperDir;
		String summaryFilename = caliperDir + CaliperUtils.SUMMARY_FILENAME;
		try {
			readSummary(summaryFilename);
			isOpen = true;
		} catch (IOException e) {
			isOpen = false;
		}
	}
	
	private void readSummary(String summaryFilename) throws IOException {
		Scanner scanner = new Scanner(new FileInputStream(summaryFilename));
		
		numProc = scanner.nextInt();
		numThread = scanner.nextInt();
		
		rankNames = new String[numThread];
		rankInfos = new RankInfo[numThread];
		
		int rankID = 0;
		for (int k = 0; k < numProc; k++) {
			int procID = scanner.nextInt();
			int procNumThread = scanner.nextInt();
			if (this.isHybridRank())
				for (int threadID = 0; threadID < procNumThread; threadID ++) {
					rankInfos[rankID] = new RankInfo(procID, threadID);
					rankNames[rankID] = procID + "." + threadID;
					rankID++;
				}
			else {
				rankInfos[rankID] = new RankInfo(procID, 0);
				rankNames[rankID] = String.valueOf(procID);
				rankID++;
			}
		}
		
		if (rankID != numThread) {
			throw new IOException(CaliperUtils.CALIPER_DATA_ERROR + 
					"number of threads doesn't match.");
		}
		
		numPhase = scanner.nextInt();
		phases = new Hashtable<String, CaliperPhase>();
		for (int i = 0; i < numPhase; i++) {
			CaliperPhase phase = new CaliperPhase(scanner.next());
			phases.put(phase.getPhaseName(), phase);
		}
		
		numLoop = scanner.nextInt();
		loops = new Hashtable<String, CaliperLoop>();
		for (int i = 0; i < numLoop; i++) {
			CaliperLoop loop = new CaliperLoop(scanner.next());
			loops.put(loop.getLoopName(), loop);
		}
		
		scanner.close();
	}
	
	public String getCaliperDir() {
		return caliperDir;
	}
	
	public boolean isCaliperDataOpen() {
		return isOpen;
	}
	
	public int getProcID(int rankID) {
		return rankInfos[rankID].procID;
	}
	
	public int getThreadID(int rankID) {
		return rankInfos[rankID].threadID;
	}
	
	public CaliperStackFrame[] getStackFrames() {
		Vector<CaliperStackFrame> all = new Vector<CaliperStackFrame>();
		all.add(CaliperUtils.CALIPER_ROOT_FRAME);
		all.addAll(phases.values());
		all.addAll(loops.values());
		
		return all.toArray(new CaliperStackFrame[0]);
	}
	
	public CaliperPhase getPhase(String phaseName) {
		return phases.get(phaseName);
	}

	public CaliperLoop getLoop(String loopName) {
		return loops.get(loopName);
	}
	
	@Override
	public String[] getListOfRanks() {
		return rankNames;
	}

	@Override
	public int getNumberOfRanks() {
		return numThread;
	}

	@Override
	public int getFirstIncluded() {
		return 0;
	}

	@Override
	public int getLastIncluded() {
		return this.getNumberOfRanks() - 1;
	}

	@Override
	public boolean isDenseBetweenFirstAndLast() {
		return true;
	}

	@Override
	public boolean isHybridRank() {
		return numProc != numThread;
	}

	@Override
	public void dispose() {
	}

}
