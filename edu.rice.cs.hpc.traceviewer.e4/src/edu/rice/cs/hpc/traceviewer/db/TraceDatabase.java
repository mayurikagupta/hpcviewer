package edu.rice.cs.hpc.traceviewer.db;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.remote.data.RemoteDBOpener;

import edu.rice.cs.hpc.traceviewer.services.DataService;

import edu.rice.cs.hpc.traceviewer.ui.OpenDatabaseDialog;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.AbstractDBOpener;
import edu.rice.cs.hpc.traceviewer.data.db.DatabaseAccessInfo;

/*************************************************************************************
 * 
 * Class to manage trace database: opening and detecting the *.hpctrace files
 * 
 *************************************************************************************/
public class TraceDatabase 
{
	
	
	/******
	 * get a new database opener
	 * 
	 * @param info
	 * @return
	 * @throws Exception 
	 */
	static private AbstractDBOpener getDBOpener(DatabaseAccessInfo info) throws Exception
	{
		AbstractDBOpener opener = null;
		if (info.isLocal())
		{
			opener = new LocalDBOpener(info);
		} else 
		{
			opener = new RemoteDBOpener(info);
		}
		return opener;
	}
	
	
	/***
	 * general static function to load a database by showing open dialog box
	 * and and display the views (if everything goes fine)
	 * 
	 * @param window
	 * @param statusMgr
	 * 
	 * @return true if the opening is successful. False otherwise
	 */
	static public boolean openRemoteDatabase(IWorkbenchWindow window) 
	{	
		OpenDatabaseDialog dlg = new OpenDatabaseDialog(window.getShell(), null, false);
		if (dlg.open() == Window.CANCEL)
			return false;
		
		DatabaseAccessInfo info = dlg.getDatabaseAccessInfo();
		return openDatabase(window, info, false);
	}

	/************
	 * static function to open a local database
	 * 
	 * @param window :  current active window
	 * @param statusMgr : current status line manager
	 * @return true if the opening is successful. False otherwise
	 */
	static public boolean openLocalDatabase(IWorkbenchWindow window, 
			final String database)
	{
		DatabaseAccessInfo info = null;
		if (database == null)
		{
			OpenDatabaseDialog dlg = new OpenDatabaseDialog(window.getShell(), null, true);
			if (dlg.open() == Window.CANCEL)
				return false;
			
			info = dlg.getDatabaseAccessInfo();
		} else {
			info = new DatabaseAccessInfo(database);
		}
		return openDatabase(window, info, true);
	}

	/*******
	 * Opening a database with a specific database access info {@link DatabaseAccessInfo}.
	 * If the opening is not successful, it tries to ask again to the user the info 
	 * 
	 * @param window
	 * @param statusMgr
	 * @param info
	 * @return
	 */
	static private boolean openDatabase(final IWorkbenchWindow window,  
			final DatabaseAccessInfo info, final boolean useLocalDatbaase)
	{
		final JobOpeningDatabase job = new JobOpeningDatabase(window, info, useLocalDatbaase);

		job.addJobChangeListener(new JobOpeningDatabaseListener(window, job));
		job.schedule();
		
		return true;
	}
	
	/*******************************************************
	 * 
	 * Class to open a database with a separate thread
	 *
	 *******************************************************/
	static private class JobOpeningDatabase extends Job 
	{
		private final IWorkbenchWindow window;
		private final DatabaseAccessInfo info; 
		private final boolean useLocalDatbaase;
		private SpaceTimeDataController stdc;
		
		JobOpeningDatabase(final IWorkbenchWindow window, final DatabaseAccessInfo info, 
				final boolean useLocalDatbaase) {
			super("Opening " + info);
			this.window = window;
			this.info	= info;
			this.stdc	= null;
			this.useLocalDatbaase = useLocalDatbaase;
		}

		@Override
		public IStatus run/*InUIThread*/(IProgressMonitor monitor) {
			DatabaseAccessInfo database_info = info;
			
			final Display display = Display.getDefault();
			do {
				try {
					AbstractDBOpener opener = getDBOpener(database_info);
					stdc = opener.openDBAndCreateSTDC(window, monitor);
				} catch (final Exception e) 
				{
					// in case of error while opening the database, we should display again
					// the open database window with the error message
					final OpenDatabaseDialog dlg = new OpenDatabaseDialog(window.getShell(), 
							e.getMessage(), useLocalDatbaase);
					
					display.syncExec( new Runnable() {

						@Override
						public void run() {
							dlg.open();
						}						
					});
					if  (dlg.getReturnCode() == Window.CANCEL)
						return Status.CANCEL_STATUS;
					stdc    	  = null; // just to mark we need to go back to the loop
					database_info = dlg.getDatabaseAccessInfo();
				}
			} while (stdc == null);
			return Status.OK_STATUS;
		}
		
		SpaceTimeDataController getSTDC() {
			return stdc;
		}
	}
	
	static private class JobOpeningDatabaseListener implements IJobChangeListener
	{
		private final IWorkbenchWindow window;
		private final JobOpeningDatabase job;
		
		JobOpeningDatabaseListener(IWorkbenchWindow window, JobOpeningDatabase job) {
			this.window = window;
			this.job	= job;
		}
		@Override
		public void sleeping(IJobChangeEvent event) {}
		
		@Override
		public void scheduled(IJobChangeEvent event) {}
		
		@Override
		public void running(IJobChangeEvent event) {}
		
		@Override
		public void done(IJobChangeEvent event) {
			if (event.getResult() == Status.OK_STATUS)
				processDatabase(window, job.getSTDC(), true);				
		}
		
		@Override
		public void awake(IJobChangeEvent event) {}
		
		@Override
		public void aboutToRun(IJobChangeEvent event) {}

	}
	
	
	static private boolean processDatabase(final IWorkbenchWindow window, 
			final SpaceTimeDataController stdc, boolean enableMidpoint)
	{
		if (stdc == null) {
			return false;
		}
		
		Display display = Display.getDefault();
		display.syncExec( new Runnable() {

			@Override
			public void run() {
				// get a window service to store the new database
				ISourceProviderService sourceProviderService = (ISourceProviderService) window.getService(ISourceProviderService.class);

				// keep the current data in "shared" variable
				DataService dataService = (DataService) sourceProviderService.getSourceProvider(DataService.DATA_PROVIDER);
				dataService.setData(stdc);

				final Shell shell = window.getShell();
				// ---------------------------------------------------------------------
				// Update the title of the application
				// ---------------------------------------------------------------------
				shell.setText("hpctraceviewer: " + stdc.getName());

				// ---------------------------------------------------------------------
				// Tell all views that we have the data, and they need to refresh
				// their content
				// Due to tightly coupled relationship between views,
				// we need to be extremely careful of the order of view activation
				// if the order is "incorrect", it can crash the program
				//
				// TODO: we need to use Eclipse's ISourceProvider to handle the
				// existence of data
				// this should avoid a tightly-coupled views
				// ---------------------------------------------------------------------
			}			
		});

		return true;

	}
}
