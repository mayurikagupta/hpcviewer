package edu.rice.cs.hpc.viewer.experiment;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.common.util.ProcedureAliasMap;
import edu.rice.cs.hpc.data.experiment.*; 
import edu.rice.cs.hpc.viewer.util.PreferenceConstants;

import edu.rice.cs.hpc.filter.service.FilterMap;

/******************************************************************************
 * Class to be used as an interface between the GUI and the data experiment
 * This class should be called from an eclipse view !
 *
 ******************************************************************************/
public class ExperimentView {

	final private Shell shell;		// workbench current page

	
	/**
	 * Constructor for Data experiment. Needed to link with the view
	 * @param objTarget: the scope view to link with
	 */
	public ExperimentView(Shell shell) {
		this.shell = shell;
	}
	
	/**
	 * A wrapper of loadExperiment() by adding some processing and generate the views
	 * @param sFilename
	 * @param bCallerView : flag to indicate if the caller view can be displayed
	 * 
	 * @return true if the experiment is loaded successfully
	 */
	public Experiment loadExperimentAndProcess(String sFilename, boolean bCallerView) {
		
		Experiment experiment = this.loadExperiment(sFilename, bCallerView);

		if(experiment != null) {
			try {			
				// check if the filter is enabled
				FilterMap filter = FilterMap.getInstance();
				if (filter.isFilterEnabled()) {
					experiment.filter(filter);
				}
		        //this.generateView(experiment);
			} catch (java.lang.OutOfMemoryError e) 
			{
				MessageDialog.openError(shell, "Out of memory", 
						"hpcviewer requires more heap memory allocation.\nJava heap size can be increased by modifying \"-Xmx\" parameter in hpcivewer.ini file.");
			} catch (java.lang.RuntimeException e) 
			{
				MessageDialog.openError(shell, "Critical error", 
						"XML file is not in correct format: \n"+e.getMessage());
				e.printStackTrace();
			}
		}
		return experiment;
	}
	
	/**
	 * A wrapper of loadExperiment() by adding some processing and generate the views
	 * The routine will first look at the user preference for displaying caller view 
	 * Then call the normal loadExperimentAndProcess routine.
	 * @param sFilename
	 */
	public Experiment loadExperimentAndProcess(String sFilename) {
		IEclipsePreferences preference = InstanceScope.INSTANCE.getNode(PreferenceConstants.P_NODE);
		boolean bCallerView = preference.getBoolean(PreferenceConstants.P_CALLER_VIEW, true);
		return this.loadExperimentAndProcess(sFilename, bCallerView);
	}
	
	/**
	 * Load an XML experiment file based on the filename (uncheck for its inexistence)
	 * This method will display errors whenever encountered.
	 * This method does not include post-processing and generating scope views
	 * @param sFilename: the xml experiment file
	 */
	public Experiment loadExperiment(String sFilename, boolean bCallerView) {
		Experiment experiment = null;
		// first view: usually already created by default by the perspective
		try
		{
			experiment = new Experiment();
			experiment.open( new java.io.File(sFilename), new ProcedureAliasMap(), bCallerView );

		} catch(java.io.FileNotFoundException fnf)
		{
			System.err.println("File not found:" + sFilename + "\tException:"+fnf.getMessage());
			MessageDialog.openError(shell, "Error:File not found", "Cannot find the file "+sFilename);
			experiment = null;
		}
		catch(java.io.IOException io)
		{
			System.err.println("IO error:" +  sFilename + "\tIO msg: " + io.getMessage());
			MessageDialog.openError(shell, "Error: Unable to read", "Cannot read the file "+sFilename);
			experiment = null;
		}
		catch(InvalExperimentException ex)
		{
			String where = sFilename + " " + " " + ex.getLineNumber();
			System.err.println("$" +  where);
			MessageDialog.openError(shell, "Incorrect Experiment File", "File "+sFilename 
					+ " has incorrect tag at line:"+ex.getLineNumber());
			experiment = null;
		} 
		catch(NullPointerException npe)
		{
			System.err.println("$" + npe.getMessage() + sFilename);
			MessageDialog.openError(shell, "File is invalid", "File has null pointer:"
					+sFilename + ":"+npe.getMessage());
			experiment = null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return experiment;
	}
	
	

}