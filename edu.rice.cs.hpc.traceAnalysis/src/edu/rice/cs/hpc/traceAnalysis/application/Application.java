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
import java.util.HashSet;
import java.util.Vector;
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
import edu.rice.cs.hpc.traceAnalysis.data.tree.Cluster.ClusterMemberID;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ClusterSetNode;
import edu.rice.cs.hpc.traceAnalysis.data.tree.RootTrace;
import edu.rice.cs.hpc.traceAnalysis.data.tree.ShadowTraceTree;
import edu.rice.cs.hpc.traceAnalysis.data.tree.TraceTree;
import edu.rice.cs.hpc.traceAnalysis.operator.ClusterIdentifier;
import edu.rice.cs.hpc.traceAnalysis.operator.LoopDetector;
import edu.rice.cs.hpc.traceAnalysis.operator.TraceFilter;
import edu.rice.cs.hpc.traceAnalysis.output.PerformanceImprovementEstimator;
import edu.rice.cs.hpc.traceAnalysis.output.RuntimeExtractor;
import edu.rice.cs.hpc.traceAnalysis.output.SignificantDiffNodePrinter;

public class Application {
	private HPCToolkitTraceReader traceReader = null;
	private final PrintStream objPrint;
	private final PrintStream objError;

	private final int printDepth = 25;
	private int nRanks;
	
	private final long startTime = System.currentTimeMillis();

	public Application(PrintStream objPrint, PrintStream objError) {
		this.objError = objError;
		this.objPrint = objPrint;
	}
	
	private String printTime() {
		long diff = System.currentTimeMillis() - startTime;
		return diff/1000 + "." + diff/100%10 + diff/10%10 + diff%10 + "s";
	}
	
	class PerRankAnalyzer implements Runnable {
		private final int rankNum;
		private final int procNum;
		private final int threadNum;
		
		PerRankAnalyzer(int rankNum) {
			this.rankNum = rankNum;
			this.procNum = traceReader.getProcNum(rankNum);
			this.threadNum = traceReader.getThreadNum(rankNum);
		}
		
		@Override
		public void run() {
			if (threadNum > 0) { 
				objError.println("Skipping building for proc #" + procNum + " thread #" + threadNum + ".");
				return;
			}
			
			TraceTree tree = null;
			synchronized (this) {
				tree = traceReader.buildTraceTree(rankNum);
			}
			objError.println("Build proc #" + procNum + " finished at " + printTime());
			//objPrint.println(tree.toString(printDepth));
			
			TraceFilter.filterTrace(tree.root);
			LoopDetector detector = new LoopDetector(tree);
			detector.detectLoop(tree.root);
			objError.println("Detect loop proc #" + procNum + " finished at " + printTime());
			//objPrint.println("*************************************************");
			//objPrint.println(tree.toString(printDepth));
			
			ClusterIdentifier clusteror = new ClusterIdentifier(Integer.toString(procNum), tree);
			clusteror.clusterLoops(tree.root);
			objError.println("Cluster proc #" + procNum + " finished at " + printTime());
			//objPrint.println("*************************************************");
			
			tree.root.setName("P" + procNum);
			
			//if (procNum <= 1) {
			//	objPrint.println(tree.printLargeDiffNodes(printDepth));
				//objPrint.println("\n\n" + tree.toString(10));
				//SignificantDiffNodePrinter.printAllCluster(objPrint, tree.root);
			//}

			try {
				FileOutputStream fileOut = new FileOutputStream("data"+File.separator+"P"+procNum);
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
	
	private boolean perRankAnalysis() {
		ExecutorService threadExecutor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors()); 

		for (int rankNum = 0; rankNum < nRanks; rankNum += 1) { 
			Runnable worker = new PerRankAnalyzer(rankNum);
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
	
	class PerRankReader implements Callable<TraceTree> {
		private final int procNum;
		private final int threadNum;
		
		PerRankReader(int rankNum) {
			this.procNum = traceReader.getProcNum(rankNum);
			this.threadNum = traceReader.getThreadNum(rankNum);
		}
		
		@Override
		public TraceTree call() throws Exception {
			if (threadNum > 0) { 
				objError.println("Skipping reading for proc #" + procNum + " thread #" + threadNum + ".");
				return null;
			}
			
			try {
				FileInputStream fileIn = new FileInputStream("data"+File.separator+"P"+procNum);
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
	
	class PerRankExtractor implements Runnable {
		private final int rankNum;
		private final int procNum;
		private final int threadNum;
		private final RuntimeExtractor extractor;
		
		PerRankExtractor(RuntimeExtractor extractor, int rankNum) {
			this.extractor = extractor;
			this.rankNum = rankNum;
			this.procNum = traceReader.getProcNum(rankNum);
			this.threadNum = traceReader.getThreadNum(rankNum);
		}
		
		@Override
		public void run() {
			if (threadNum > 0) { 
				objError.println("Skipping building for proc #" + procNum + " thread #" + threadNum + ".");
				return;
			}
			
			TraceTree tree = null;
			synchronized (this) {
				tree = traceReader.buildTraceTree(rankNum);
			}
			objError.println("Build proc #" + procNum + " finished at " + printTime());
			
			TraceFilter.filterTrace(tree.root);
			LoopDetector detector = new LoopDetector(tree);
			detector.detectLoop(tree.root);
			objError.println("Detect loop proc #" + procNum + " finished at " + printTime());
			
			extractor.extractTime(procNum, tree.root);
		}
	}
	
	
	private boolean crossRankAnalysis() {
		objError.println("\n\nMerging all ranks at " + printTime());
		
		RootTrace root = new RootTrace("Root for all ranks");
		root.getTraceTime().setStartTimeExclusive(0);
		root.getTraceTime().setStartTimeInclusive(0);
		root.getTraceTime().setEndTimeInclusive(traceReader.getDurantion());
		root.getTraceTime().setEndTimeExclusive(traceReader.getDurantion());
		root.initDurationRep();
		
		
		for (int rankNum = 0; rankNum < nRanks; rankNum += 1) { 
			int procNum = traceReader.getProcNum(rankNum);
			int threadNum = traceReader.getThreadNum(rankNum);
			
			if (threadNum > 0) { 
				objError.println("Skipping merging for proc #" + procNum + " thread #" + threadNum + ".");
				continue;
			}
			
			ShadowTraceTree shadow = new ShadowTraceTree("data"+File.separator+"P"+procNum);
			root.addChild(shadow);
		}
		
		
		/*objError.println("\n\nReading all ranks at " + printTime());
		
		ExecutorService threadExecutor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors()); 
		ArrayList<Future<TraceTree>> futures = new ArrayList<Future<TraceTree>>();
		for (int rankNum = 0; rankNum < nRanks; rankNum += 1) { 
			PerRankReader reader = new PerRankReader(rankNum);
			Future<TraceTree> future = threadExecutor.submit(reader);
			futures.add(future);
		}

		
		for (Future<TraceTree> future : futures) {
			try {
				TraceTree tree = future.get();
				if (tree != null) root.addChild(tree.root);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		threadExecutor.shutdown();*/
		
		
		root.setDepth(0);
		
		ClusterIdentifier clusteror = new ClusterIdentifier(null, null);
		ClusterSetNode node = (ClusterSetNode) clusteror.findCluster(root, Runtime.getRuntime().availableProcessors());
		
		objError.println("\n\nFinished merge all ranks at " + printTime());
	/*	
		try {
			FileOutputStream fileOut = new FileOutputStream("summary");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(node);
			out.close();
			fileOut.close();
		} catch (IOException e) {
			objError.println("file not found when outputing summary.");
			e.printStackTrace();
			return false;
		}
		
		objPrint.println();
		
		return true;
	}
	
	private boolean summaryAnalysis() {
		ClusterSetNode node = null;
		*/
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
		
		objError.println("Analyzing summary at " + printTime());
		/*
		try {
			FileInputStream fileIn = new FileInputStream("summary");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			node = (ClusterSetNode) in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException e) {
			objError.println("file not found when reading summary");
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException c) {
	         objError.println("class not found when reading summary");
	         c.printStackTrace();
	         return false;
	    }
		*/
		
		objPrint.println(node.printLargeDiffNodes(printDepth, 0, Long.MIN_VALUE));
		PerformanceImprovementEstimator callpathPrinter = new PerformanceImprovementEstimator(objPrint, node);
		callpathPrinter.printImprovementReport();
		
		Vector<HashSet<Integer>> cluster = new Vector<HashSet<Integer>>();
		for (int i = 0; i < node.getNumOfClusters(); i++) {
			cluster.add(new HashSet<Integer>());
			for (ClusterMemberID member : node.getCluster(i).getMembers()) {
				cluster.get(i).add(Integer.valueOf(member.toString()));
			}
		}
		
		node = null;
		
		RuntimeExtractor extractor = new RuntimeExtractor(root.getNumOfChildren(), callpathPrinter.getSignificantCallpaths(), cluster);
		ExecutorService threadExecutor = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors()); 

		for (int rankNum = 0; rankNum < nRanks; rankNum += 1) { 
			Runnable worker = new PerRankExtractor(extractor, rankNum);
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
		
		extractor.printTime(objError);
		
		objError.println("Exit at " + printTime());
		
		return true;
	}
	
	private boolean openExperiment(File objFile) {
		if (!objFile.canRead()) return false;
		
		objError.println("Started at " + printTime());
		
		this.traceReader = new HPCToolkitTraceReader(objFile, objPrint, objError);
		this.nRanks = traceReader.getNumberOfRanks();
		objError.println("Init finished at " + printTime());
		
		objError.println("Num of avail procs = " + Runtime.getRuntime().availableProcessors());
		
		//if (!this.perRankAnalysis()) return false;
		if (!this.crossRankAnalysis()) return false;
		//if (!this.summaryAnalysis()) return false;
		
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
