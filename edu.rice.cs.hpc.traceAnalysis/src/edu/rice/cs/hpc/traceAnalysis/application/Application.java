package edu.rice.cs.hpc.traceAnalysis.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.data.util.Util;

import edu.rice.cs.hpc.traceAnalysis.data.HPCToolkitTraceReader;
import edu.rice.cs.hpc.traceAnalysis.data.TraceTree;
import edu.rice.cs.hpc.traceAnalysis.iteration.LoopDetector;

public class Application {
	private boolean openExperiment(PrintStream objPrint, PrintStream objError, File objFile) {
		if (!objFile.canRead()) return false;
		
		HPCToolkitTraceReader traceReader = new HPCToolkitTraceReader(objFile, objPrint, objError);
		/*for (int i = 0; i < traceReader.getNumberOfRanks(); i++) {
			//traceReader.readRank(i);
			objPrint.println(traceReader.buildTraceTree(i).toString());
		}*/
		
		//traceReader.readRank(0);
		TraceTree tree = traceReader.buildTraceTree(0);
		//objPrint.println(traceReader.buildTraceTree(0).print(2));
		
		LoopDetector detector = new LoopDetector();
		detector.detectLoop(tree.root, 0, tree.numSamples);
		
		//objPrint.println(traceReader.buildTraceTree(1).print(2));
		
		return true;
	}
	
	public static void main(String[] args) {
		Application objApp = new Application();
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
		
		File objFile = new File(sFilename);
		boolean done = false;

		print_msg.println("Opening database " + sFilename);
		
		if (objFile.isDirectory()) {
			File files[] = Util.getListOfXMLFiles(sFilename);
			for (File file: files) 
			{
				// only experiment*.xml will be considered as database file
				if (file.getName().startsWith(Constants.DATABASE_FILENAME)) {
					done = objApp.openExperiment(objPrint, print_msg, file);
					if (done)
						break;
				}
			}
		} else {
			done = objApp.openExperiment(objPrint, print_msg, objFile);
			
		}

		if (done)
			print_msg.println("Application ended successfully");
	}

}
