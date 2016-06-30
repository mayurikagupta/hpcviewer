package edu.rice.cs.hpc.traceviewer.db.local;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.InvalExperimentException;
import edu.rice.cs.hpc.data.experiment.extdata.FileDB2;
import edu.rice.cs.hpc.data.experiment.extdata.IFileDB;
import edu.rice.cs.hpc.data.util.Util;
import edu.rice.cs.hpc.traceviewer.data.controller.TraceDataController;
import edu.rice.cs.hpc.traceviewer.data.db.AbstractDBOpener;
import edu.rice.cs.hpc.traceviewer.data.db.DatabaseAccessInfo;
import edu.rice.cs.hpc.traceviewer.data.version3.FileDB3;

/*******************************************************************
 * 
 * Class to open a local database
 * 
 * @author Philip Taffet
 * 
 *******************************************************************/
public class LocalDBOpener extends AbstractDBOpener 
{
	private String directory;
	private int version;
	
	/*******
	 * prepare opening a database 
	 * 
	 * @param directory : the directory of the database
	 * @throws Exception 
	 */
	public LocalDBOpener(DatabaseAccessInfo info) throws Exception
	{
		this.directory = info.getDatabasePath();
		version = LocalDBOpener.directoryHasTraceData(directory); 
		if (version<=0) {
			throw new Exception("The directory does not contain hpctoolkit database with trace data:"
					+ directory);
		}
	}
	
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.traceviewer.db.AbstractDBOpener#openDBAndCreateSTDC
	 * (org.eclipse.ui.IWorkbenchWindow, org.eclipse.jface.action.IStatusLineManager)
	 */
	public TraceDataController openDBAndCreateSTDC(IWorkbenchWindow window,
			final IProgressMonitor statusMgr) throws InvalExperimentException, Exception {
			
		// Laks 2014.03.10: needs to comment the call to removeInstance
		// this call causes the data to be deleted but the GC+Color instances still exist
		// the allocated GC+Color can be disposed later in SpaceTimeDataController class
		
		//	TraceDatabase.removeInstance(window);

		// ---------------------------------------------------------------------
		// Try to open the database and refresh the data
		// ---------------------------------------------------------------------
		
		
		statusMgr.setTaskName("Opening trace data...");

		// ---------------------------------------------------------------------
		// dispose resources if the data has been allocated
		// unfortunately, some colors are allocated from window handle,
		// some are allocated dynamically. At the moment we can't dispose
		// all colors
		// ---------------------------------------------------------------------
		// if (database.dataTraces != null)
		// database.dataTraces.dispose();

		IFileDB fileDB;
		switch (version)
		{
		case 1:
		case 2:
			fileDB = new FileDB2();
			break;
		case 3:
			fileDB = new FileDB3();
			break;
		default:
			throw new InvalExperimentException("Trace data version is not unknown: " + version);
		}
		//fileDB.open(directory, 0, 0);
		
		// prepare the xml experiment and all extended data
		SpaceTimeDataControllerLocal stdc = new SpaceTimeDataControllerLocal(
				window, statusMgr, directory, fileDB);
		
		// open trace data with the default record size (since the version 2.0 has no info)
		// fortunately version 3 and 2 have the same record size.
		//fileDB.open(directory, stdc.getHeaderSize(), SpaceTimeDataControllerLocal.RECORD_SIZE);
		
		return stdc;
	}

	/**********************
	 * static method to check if a directory contains hpctoolkit's trace data
	 * 
	 * @param directory : a database directory
	 * @return int version of the database if the database is correct and valid
	 * 			   return negative number otherwise
	 */
	static private int directoryHasTraceData(String directory)
	{
		File file = new File(directory);
		String database_directory;
		if (file.isFile()) {
			// if the argument is a file, then we'll look for its parent directory
			file = file.getParentFile();
			database_directory = file.getAbsolutePath();
		} else {
			database_directory = directory;
		}
		// checking for version 3.0
		String file_path = database_directory + File.separatorChar + "trace.db";
		File tmp_file 	 = new File(file_path);
		if (tmp_file.canRead()) {
			return 3;
		}
		
		// checking for version 2.0
		file_path = database_directory + File.separatorChar + "experiment.mt";
		tmp_file  = new File(file_path);
		if (tmp_file.canRead()) {
			return 2;
		}
		
		// checking for version 2.0 with old format files
		tmp_file  = new File(database_directory);
		File[] file_hpctraces = tmp_file.listFiles( new Util.FileThreadsMetricFilter("*.hpctrace") );
		if (file_hpctraces != null && file_hpctraces.length>0) {
			return 1;
		}
		return -1;
	}

	@Override
	public void end() {
	}	
}


