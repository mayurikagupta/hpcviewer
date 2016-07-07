package edu.rice.cs.hpc.traceviewer.data.caliper.test;

import java.io.*;

import edu.rice.cs.hpc.traceviewer.data.caliper.CaliperUtils;
import edu.rice.cs.hpc.traceviewer.data.caliper.ProcessCaliperData;
import edu.rice.cs.hpc.traceviewer.data.caliper.db.CaliperDataSummary;
import edu.rice.cs.hpc.traceviewer.data.caliper.stackframe.CaliperStackFrame;

public class CalipterTest {
	public static void main(String[] args) {
		PrintStream objPrint = System.out;
		String sFilename;
		boolean std_output = true;
		
		//------------------------------------------------------------------------------------
		// processing the command line argument
		//------------------------------------------------------------------------------------
		if ( (args == null) || (args.length==0)) {
			System.out.println("Usage: hpcdata.sh [-o output_file] experiment_database");
			return;
		} else  {
			sFilename = args[0];
			
			for (int i=0; i<args.length; i++) {
				if (args[i].equals("-o") && (i<args.length-1)) {
					String sOutput = args[i+1];
					File f = new File(sOutput);
					if (!f.exists())
						try {
							f.createNewFile();
						} catch (IOException e1) {
							e1.printStackTrace();
							return;
						}
					try {
						FileOutputStream file = new FileOutputStream(sOutput);
						try {
							objPrint = new PrintStream( file );
							std_output = false;
							i++;
						} catch (Exception e) {
							System.err.println("Error: cannot create file " + sOutput + ": " +e.getMessage());
							return;
						}

					
					} catch (FileNotFoundException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				} else {
					sFilename = args[i];
				}
			}
		}
		
		//------------------------------------------------------------------------------------
		// open the experiment if possible
		//------------------------------------------------------------------------------------
		PrintStream print_msg;
		if (std_output)
			print_msg = System.err;
		else
			print_msg = System.out;

		print_msg.println("Opening tracefile " + sFilename);
		
		CaliperDataSummary summary = new CaliperDataSummary(sFilename + File.separatorChar + CaliperUtils.CALIPER_DIR);

		if (summary.isCaliperDataOpen()) {
			for (String s : summary.getListOfRanks())
				objPrint.println(s);
			
			for (CaliperStackFrame f : summary.getStackFrames()) 
				objPrint.println(f.getName());
		}
		
		ProcessCaliperData data = new ProcessCaliperData(0, summary, 0, 1467828121246621L, 90000000L, 256);
		try {
			data.readInData();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*
		try {
			DatabaseAccessInfo info = new DatabaseAccessInfo(sFilename);
			LocalDBOpenerForTraceSummary opener = new LocalDBOpenerForTraceSummary(info);
			SpaceTimeDataControllerLocalForTraceSummary stdc = opener.openDBAndCreateSTDC(print_msg);
			
			//stdc.printTraceSummaries(objPrint);
			
			NeighborJoining nj = new NeighborJoining(stdc.getTraceSummaries(print_msg));
			nj.printNJResult(objPrint);
			
		} catch (Exception e) {
			print_msg.println("Fail to process the database. " + e.getMessage());
			e.printStackTrace();
		}*/
		
		
		print_msg.println("Finished.");
	}
}
