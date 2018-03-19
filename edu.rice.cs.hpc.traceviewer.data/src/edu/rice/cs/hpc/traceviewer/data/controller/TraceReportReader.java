package edu.rice.cs.hpc.traceviewer.data.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import org.eclipse.swt.graphics.RGB;

import edu.rice.cs.hpc.traceviewer.data.graph.CallPathColorTable;

public class TraceReportReader {
	private boolean isValid;
	private boolean isCluster;
	
	int[] callpathID;
	String[] callpathType;
	
	String[][] clusters;
	
	TraceReportReader(String expDir) {
		String filename = expDir + File.separator + "trace_report.txt";
		try {
			readReport(filename);
			isValid = true;
		} catch (IOException e) {
			isValid = false;
		}
	}
	
	private void readReport(String filename) throws IOException {
		Scanner scanner = new Scanner(new FileInputStream(filename));
		int size = scanner.nextInt();
		
		callpathID = new int[size];
		callpathType = new String[size];
		
		for (int i = 0; i < size; i++) {
			callpathID[i] = scanner.nextInt();
			callpathType[i] = scanner.next();
		}
		
		if (scanner.hasNextInt()) {
			isCluster = true;
			int numCluster = scanner.nextInt();
			scanner.nextLine();
			
			clusters = new String[numCluster][];
			for (int k = 0; k < numCluster; k++) {
				String str = scanner.nextLine();
				clusters[k] = str.split(", ");
				/*for (int i = 0; i < clusters[k].length; i++) {
					System.out.print(clusters[k][i] + ".");
				}
				System.out.println();*/
			}
		} else isCluster = false;
		
		scanner.close();
	}
	
	public boolean isValid() {
		return this.isValid;
	}
	
	public boolean isCluster() {
		return this.isCluster;
	}
	
	
	public void colorCallpaths(CallPathColorTable colorTable) {
		Random r = new Random((long)189691069);
		int v1,v2,v3;
		
		int min = 128;
		for (int i = 0; i < callpathID.length; i++) {
			RGB color = null;
			if (callpathType[i].contains("C")) { // red for computation
				do {
					v1 = r.nextInt(256-min) + min;
					v2 = r.nextInt(256-min/2) + min/2;
					v3 = r.nextInt(256-min/2) + min/2;
				} while ((v1 < v2) || (v1 < v3 * 1.5));
				color = new RGB(v1, v2, v3);
			}
			else if (callpathType[i].contains("W")) { // green or light blue for wait
				do {
					v1 = r.nextInt(256-min) + min;
					v2 = r.nextInt(256-min/2) + min/2;
					v3 = r.nextInt(256-min/2) + min/2;
				} while ((v1 < v2) || (v1 < v3 * 1.5));
				color = new RGB(v3, v1, v2);
			}
			else {								// purple for sync
				do {
					v1 = r.nextInt(256-min) + min;
					v2 = r.nextInt(256-min) + min;
					v3 = r.nextInt(256-min/2) + min/2;
				} while ((v1 > v2 + min/2) || (v2 > v1 + min/2) || (v1 < v3 * 1.5) || (v2 < v3 * 1.5));
				color = new RGB(v2, v3, v1);
			}
			
			colorTable.setColor(callpathID[i], color);
		}
	}
	
	public String[][] getClusters() {
		return clusters;
	}
}
