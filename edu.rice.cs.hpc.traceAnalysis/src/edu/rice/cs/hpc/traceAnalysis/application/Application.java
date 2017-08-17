package edu.rice.cs.hpc.traceAnalysis.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.data.util.Util;

import edu.rice.cs.hpc.traceAnalysis.data.reader.HPCToolkitTraceReader;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.RootTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTree;
import edu.rice.cs.hpc.traceAnalysis.operator.ClusterIdentifier;
import edu.rice.cs.hpc.traceAnalysis.operator.LoopDetector;
import edu.rice.cs.hpc.traceAnalysis.operator.TraceFilter;
import edu.rice.cs.hpc.traceAnalysis.output.SignificantDiffNodePrinter;

public class Application {
	private boolean openExperiment(PrintStream objPrint, PrintStream objError, File objFile) {
		if (!objFile.canRead()) return false;
		
		objError.println("Start at " + (new Date()).toString());
		
		HPCToolkitTraceReader traceReader = new HPCToolkitTraceReader(objFile, objPrint, objError);
		
		objError.println("Init finished at " + (new Date()).toString());
		
		int printDepth = 25;
		
		RootTrace root = new RootTrace("Root for all procs");
		root.getTime().setStartTimeExclusive(0);
		root.getTime().setStartTimeInclusive(0);
		root.getTime().setEndTimeInclusive(traceReader.getDurantion());
		root.getTime().setEndTimeExclusive(traceReader.getDurantion());
		
		for (int i = 0; i < 24; i += 6) { 
			objError.println("\n\nProcessing traceline #" + i + ": ");
			TraceTree tree = traceReader.buildTraceTree(i);
			objError.println("Build finished at " + (new Date()).toString());
			//objPrint.println(tree.toString(printDepth));
			
			TraceFilter.filterTrace(tree.root);
			LoopDetector detector = new LoopDetector(tree);
			detector.detectLoop(tree.root);
			objError.println("Detect loop finished at " + (new Date()).toString());
			//objPrint.println("*************************************************");
			//objPrint.println(tree.toString(printDepth));
			ClusterIdentifier.clusterLoops(tree.root);
			objError.println("Cluster finished at " + (new Date()).toString());
			//objPrint.println("*************************************************");
			if (i == 0) {
				objPrint.println(tree.printLargeDiffNodes(printDepth));
				SignificantDiffNodePrinter.printAllCluster(objPrint, tree.root);
			}
			
			tree.root.setName("PROC_#" + i);
			root.addChild(tree.root, tree.root.getTime(), null);
		}
		
		objError.println("\n\nMerging all threads at " + (new Date()).toString());
		root.setDepth(0);
		//objPrint.println(root.toString(printDepth, 10000, 0));
		
		
		AbstractTreeNode node = ClusterIdentifier.findCluster(root);
		//objPrint.println(node.toString(10, 10000, 2));
		objPrint.println(node.printLargeDiffNodes(printDepth, 0, null, Long.MIN_VALUE));
		SignificantDiffNodePrinter.printAllCluster(objPrint, node);
		
		objError.println("Exit at " + (new Date()).toString());
		/*
		ClusterNode cluster = ClusterIdentifier.findCluster(root);
		objPrint.println(cluster.print(3, 0));
		*/
		
		/*
		//traceReader.readRank(0);
		TraceTree tree = traceReader.buildTraceTree(0);
		
		LoopDetector detector = new LoopDetector(tree);
		detector.detectLoop(tree.root);
		
		//objPrint.println(tree.print(4));
		//objPrint.println(detector.detectedLoopID);
		
		//ProfileNode prof = Trace2ProfileConverter.trace2profile(((AbstractTraceNode)tree.root.getChild(0)).getChild(1));
		//objPrint.println(((AbstractTraceNode)tree.root.getChild(0)).getChild(1).print(4, 0));
		//objPrint.println(prof.print(7, 1000));
		
		//System.out.println("***********************************************");
		IterationClassifier.ClasifyLoops(tree.root);
		objPrint.println(tree.print(6));
		*/
		
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
