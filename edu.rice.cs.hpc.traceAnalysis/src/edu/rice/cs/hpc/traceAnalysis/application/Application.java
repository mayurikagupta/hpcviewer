package edu.rice.cs.hpc.traceAnalysis.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.data.util.Util;

import edu.rice.cs.hpc.traceAnalysis.data.reader.HPCToolkitTraceReader;
import edu.rice.cs.hpc.traceAnalysis.data.tree.AbstractTreeNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterSetNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.RootTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ShadowTraceTree;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTree;
import edu.rice.cs.hpc.traceAnalysis.operator.ClusterIdentifier;
import edu.rice.cs.hpc.traceAnalysis.operator.LoopDetector;
import edu.rice.cs.hpc.traceAnalysis.operator.TraceFilter;
import edu.rice.cs.hpc.traceAnalysis.output.PerformanceImprovementEstimator;
import edu.rice.cs.hpc.traceAnalysis.output.SignificantDiffNodePrinter;

public class Application {
	private HPCToolkitTraceReader traceReader = null;
	private final PrintStream objPrint;
	private final PrintStream objError;

	private final int printDepth = 25;
	private final int nProc = 12;
	
	private final long startTime = System.currentTimeMillis();

	public Application(PrintStream objPrint, PrintStream objError) {
		this.objError = objError;
		this.objPrint = objPrint;
	}
	
	private String printTime() {
		long diff = System.currentTimeMillis() - startTime;
		return diff/1000 + "." + diff/100%10 + diff/10%10 + diff%10 + "s";
	}
	
	class PerThreadAnalyzer implements Runnable {
		private final int procNum;
		
		PerThreadAnalyzer(int procNum) {
			this.procNum = procNum;
		}
		
		@Override
		public void run() {
			TraceTree tree = null;
			synchronized (this) {
				tree = traceReader.buildTraceTree(procNum);
			}
			objError.println("Build traceline #" + procNum + " finished at " + printTime());
			//objPrint.println(tree.toString(printDepth));
			
			TraceFilter.filterTrace(tree.root);
			LoopDetector detector = new LoopDetector(tree);
			detector.detectLoop(tree.root);
			objError.println("Detect loop traceline #" + procNum + " finished at " + printTime());
			//objPrint.println("*************************************************");
			//objPrint.println(tree.toString(printDepth));
			
			ClusterIdentifier clusteror = new ClusterIdentifier(procNum, tree);
			clusteror.clusterLoops(tree.root);
			objError.println("Cluster traceline #" + procNum + " finished at " + printTime());
			//objPrint.println("*************************************************");
			
			if (procNum == 0) {
				objPrint.println(tree.printLargeDiffNodes(printDepth));
				objPrint.println(tree.toString(10));
				SignificantDiffNodePrinter.printAllCluster(objPrint, tree.root);
			}
			
			tree.root.setName("P" + procNum);
			
			try {
				FileOutputStream fileOut = new FileOutputStream("data\\P"+procNum);
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
				out.writeObject(tree);
				out.close();
				fileOut.close();
			} catch (IOException e) {
				objError.println("file not found when outputing analyzed trace tree for proc #" + procNum);
				e.printStackTrace();
			}
		}
	}
	
	private boolean perThreadAnalysis() {
		ExecutorService threadExecutor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors()); 

		for (int procNum = 0; procNum < nProc; procNum += 1) { 
			Runnable worker = new PerThreadAnalyzer(procNum);
			threadExecutor.execute(worker);
		}
		threadExecutor.shutdown();
		
		try {
			while (!threadExecutor.awaitTermination(60, TimeUnit.SECONDS)) {}
		} catch (InterruptedException e1) {
			objError.println("Analysis thread interrupted!");
			e1.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	class PerThreadReader implements Callable<TraceTree> {
		private final int procNum;
		
		PerThreadReader(int procNum) {
			this.procNum = procNum;
		}
		
		@Override
		public TraceTree call() throws Exception {
			try {
				FileInputStream fileIn = new FileInputStream("data\\P"+procNum);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				TraceTree tree = (TraceTree) in.readObject();
				in.close();
				fileIn.close();
				return tree;
			} catch (IOException e) {
				objError.println("file not found when reading analyzed trace tree for proc #" + procNum);
				e.printStackTrace();
				return null;
			} catch (ClassNotFoundException c) {
		         objError.println("class not found when reading analyzed trace tree for proc #" + procNum);
		         c.printStackTrace();
		         return null;
		    }
		}
		
	}
	
	private boolean crossThreadAnalysis() {
		objError.println("\n\nReading all threads at " + printTime());
		
		
		ExecutorService threadExecutor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors()); 
		ArrayList<Future<TraceTree>> futures = new ArrayList<Future<TraceTree>>();
		for (int procNum = 0; procNum < nProc; procNum += 1) { 
			PerThreadReader reader = new PerThreadReader(procNum);
			Future<TraceTree> future = threadExecutor.submit(reader);
			futures.add(future);
		}
		
		
		RootTrace root = new RootTrace("Root for all procs");
		root.getTraceTime().setStartTimeExclusive(0);
		root.getTraceTime().setStartTimeInclusive(0);
		root.getTraceTime().setEndTimeInclusive(traceReader.getDurantion());
		root.getTraceTime().setEndTimeExclusive(traceReader.getDurantion());
		root.initDurationRep();
		
		/*
		for (int procNum = 0; procNum < nProc; procNum += 1) { 
			ShadowTraceTree shadow = new ShadowTraceTree("data\\P"+procNum);
			root.addChild(shadow, null, null);
		}*/
		
		
		for (Future<TraceTree> future : futures) {
			try {
				TraceTree tree = future.get();
				root.addChild(tree.root);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		threadExecutor.shutdown();
		
		
		objError.println("\n\nMerging all threads at " + printTime());
		root.setDepth(0);
		
		ClusterIdentifier clusteror = new ClusterIdentifier(-1, null);
		ClusterSetNode node = (ClusterSetNode) clusteror.findCluster(root, 1); //Runtime.getRuntime().availableProcessors());
		
		objError.println("\n\nFinished all threads at " + printTime());
		
		objPrint.println();
		objPrint.println();
		
		/*
		double maxDiffRatio = 0;
		for (int i = 0; i < node.getNumOfClusters(); i++)
			for (int j = i+1; j < node.getNumOfClusters(); j++) {
				AbstractTreeNode diff = clusteror.mergeNode(node.getCluster(i).getRep(), node.getCluster(i).getWeight(), 
						node.getCluster(j).getRep(), node.getCluster(j).getWeight(), false, true);
				double ratio = diff.getInclusiveDiffScore() / node.getCluster(i).getWeight() / node.getCluster(j).getWeight() / 
						(node.getCluster(i).getRep().getDuration() + node.getCluster(j).getRep().getDuration());
				maxDiffRatio = Math.max(maxDiffRatio, ratio);
			}
		objPrint.println("Max diff ratio = " + String.format("%.2f", maxDiffRatio*100) + "%");
		//objPrint.println(node.toString(10, 10000, 2));
		objPrint.println(node.printLargeDiffNodes(printDepth, 0, Long.MIN_VALUE));
		
		SignificantDiffNodePrinter.printAllCluster(objPrint, node);
		*/
		
		PerformanceImprovementEstimator.printSignificantImprovement(objPrint, node);
		
		objError.println("Exit at " + printTime());
		
		return true;
	}
	
	private boolean openExperiment(File objFile) {
		if (!objFile.canRead()) return false;
		
		objError.println("Started at " + printTime());
		
		this.traceReader = new HPCToolkitTraceReader(objFile, objPrint, objError);
		objError.println("Init finished at " + printTime());
		
		if (!this.perThreadAnalysis()) return false;
		if (!this.crossThreadAnalysis()) return false;

		return true;
	}
	
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
		
		Application objApp = new Application(objPrint, print_msg);
		
		File objFile = new File(sFilename);
		boolean done = false;

		print_msg.println("Opening database " + sFilename);
		
		if (objFile.isDirectory()) {
			File files[] = Util.getListOfXMLFiles(sFilename);
			for (File file: files) 
			{
				// only experiment*.xml will be considered as database file
				if (file.getName().startsWith(Constants.DATABASE_FILENAME)) {
					done = objApp.openExperiment(file);
					if (done)
						break;
				}
			}
		} else {
			done = objApp.openExperiment(objFile);
			
		}

		if (done)
			print_msg.println("Application ended successfully");
	}

}
