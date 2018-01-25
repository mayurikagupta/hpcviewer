package edu.rice.cs.hpc.traceviewer.data.version2;

import java.util.HashMap;

import edu.rice.cs.hpc.data.experiment.extdata.IFileDB;
import edu.rice.cs.hpc.traceviewer.data.controller.TraceReportReader;

public class ClusteredBaseData extends AbstractBaseData {
	final private TraceReportReader reader;
	
	int numFakeRank;
	String listOfFakeRank[];
	int fakeRankMap[]; // map fake rank number to the true rank number
	
	public ClusteredBaseData(IFileDB baseDataFile, TraceReportReader reader) {
		super(baseDataFile);
		this.reader = reader;
		init();
	}
	
	private void init() {
		// map rank names into rank number
		HashMap<String, Integer> nameToRank = new HashMap<String, Integer>();
		String[] rankLabels = baseDataFile.getRankLabels();
		for (int i = 0; i < rankLabels.length; i++) {
			nameToRank.put(rankLabels[i], i);
			
			// if a rank name is "*.0" (threadNum is 0), we also add "*" to the map
			if (rankLabels[i].contains(".") && rankLabels[i].substring(rankLabels[i].indexOf(".")+1).equals("0"))
				nameToRank.put(rankLabels[i].substring(0, rankLabels[i].indexOf(".")), i);
		}
		
		String[][] clusters = reader.getClusters();
		// find the biggest cluster
		int max = 0;
		for (String[] cluster : clusters)
			max = Math.max(max, cluster.length);
		
		numFakeRank = 0;
		for (String[] cluster : clusters) {
			int num = cluster.length;
			int fakeNum = (int) Math.round(max * Math.log(num+1) / Math.log(max+1) / num);
			
			numFakeRank += fakeNum * num;
		}
		
		listOfFakeRank = new String [numFakeRank];
		fakeRankMap = new int [numFakeRank];
		
		int count = 0;
		for (String[] cluster : clusters) {
			int num = cluster.length;
			int fakeNum = (int) Math.round(max * Math.log(num+1) / Math.log(max+1) / num);
			
			for (int k = 0; k < num; k++)
				for (int i = 0; i < fakeNum; i++) {
					listOfFakeRank[count] = cluster[k];
					fakeRankMap[count] = nameToRank.get(cluster[k]);
					count++;
				}
		}
		
		//for (int i = 0; i < numFakeRank; i++)
		//System.out.println(i + ": " + listOfFakeRank[i] + " " + fakeRankMap[i]);
	}

	@Override
	public long getMinLoc(int rank) {
		return baseDataFile.getMinLoc(fakeRankMap[rank]);
	}

	@Override
	public long getMaxLoc(int rank) {
		return baseDataFile.getMaxLoc(fakeRankMap[rank]);
	}

	@Override
	public String[] getListOfRanks() {
		return listOfFakeRank;
	}

	@Override
	public int getNumberOfRanks() {
		return numFakeRank;
	}

	@Override
	public int getFirstIncluded() {
		return 0;
	}

	@Override
	public int getLastIncluded() {
		return numFakeRank-1;
	}

	@Override
	public boolean isDenseBetweenFirstAndLast() {
		return true;//No filtering
	}

}
