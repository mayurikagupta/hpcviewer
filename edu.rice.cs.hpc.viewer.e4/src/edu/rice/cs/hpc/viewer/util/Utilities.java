/**
 * 
 */
package edu.rice.cs.hpc.viewer.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IWorkbenchWindow;
import edu.rice.cs.hpc.data.experiment.source.FileSystemSourceFile;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScopeType;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.util.OSValidator;

import edu.rice.cs.hpc.viewer.resources.ResourceConstant;
import edu.rice.cs.hpc.viewer.resources.ResourceProvider;



/**
 * Class providing auxiliary utilities methods.
 * Remark: it is useless to instantiate this class since all its methods are static !
 * @author laksono
 *
 */
public class Utilities {
	//special font for the metric columns. It supposed to be fixed font
	static public Font fontMetric;
	/* generic font for view and editor */
	static public Font fontGeneral;
	
	// special color for the top row
	static public Color COLOR_TOP;
	
	static public String NEW_LINE = System.getProperty("line.separator");
	
	static private ResourceProvider resource;
	
	/**
	 * Set the font for the metric columns (it may be different to other columns)
	 * This method has to be called first before others
	 * @param display
	 */
	static public void setFontMetric(Display display) {
		COLOR_TOP = new Color(display, 255,255,204);
		IEclipsePreferences preference = InstanceScope.INSTANCE.getNode(PreferenceConstants.P_NODE);

		FontData []objFontMetric = display.getSystemFont().getFontData();
		FontData []objFontGeneric = display.getSystemFont().getFontData();
		
		if (OSValidator.isWindows())
			// On Windows 7 Courier New has better look 
			objFontMetric[0].setName("Courier New"); 
		else
			// For most platforms, Courier is fine 
			objFontMetric[0].setName("Courier"); 

		// get the font for metrics columns based on user preferences
		if (preference != null) {
			// bug fix: for unknown reason, the second instance of hpcviewer cannot find the key
			//	solution: check if the key exist or not IPreferenceStore.STRING_DEFAULT_DEFAULT.equals
			String sValue = preference.get(PreferenceConstants.P_FONT_METRIC, null); 
			if ( !IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(sValue) ) {
				// convert the preference into a metric font 
				
			} else {
				// bug fix: if user hasn't set the preference, we set it for him/her

			}
			sValue = preference.get(PreferenceConstants.P_FONT_GENERIC, null);
			if ( !IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(sValue) ) {
				// convert the preference into a generic font 
			} else {
				// bug fix: if user hasn't set the preference, we set it for him/her

			}
		}
		// create font for general purpose (view, editor, ...)
		Utilities.fontGeneral = new Font (display, objFontGeneric);
		
		Utilities.fontMetric = new Font(display, objFontMetric);
	}

	/**
	 * Set a new font for metric and generic view
	 * @param window
	 * @param objFontMetric
	 * @param objFontGeneric
	 */
	static public void setFontMetric(IWorkbenchWindow window, FontData objFontMetric[], FontData objFontGeneric[]) {
		FontData []myFontMetric = Utilities.fontMetric.getFontData();
		boolean isMetricFontChanged = isDifferentFontData(myFontMetric, objFontMetric);
		if (isMetricFontChanged) {
			Device device = Utilities.fontMetric.getDevice();
			Utilities.fontMetric.dispose();
			Utilities.fontMetric = new Font(device, objFontMetric);
		}
		
		FontData []myFontGeneric = Utilities.fontGeneral.getFontData();
		boolean isGenericFontChange = isDifferentFontData(myFontGeneric, objFontGeneric);
		if (isGenericFontChange) {
			Device device = Utilities.fontGeneral.getDevice();
			Utilities.fontGeneral.dispose();
			Utilities.fontGeneral = new Font(device, objFontGeneric);
		}
		
		if (isMetricFontChanged || isGenericFontChange) {
			// a font has been changed. we need to refresh the view
			resetAllViews (window);
			
			// refresh other windows too

			// set the fonts in the preference store to the new fonts
			// if we got here from a preference page update this was already done by the font field editor but 
			// if we got here from a tool bar button then we need the new values to be put into the preference store
			// in addition this call will fire a property changed event which other non-Rice views can listen for and
			// use it to refresh their views (without this event the SWT code throws lots of invalid argument exceptions
			// because it tries to repaint the non-Rice views using the font that the Rice code has just disposed).
			Utilities.storePreferenceFonts();
		}
	}
	
	
	/*****
	 * check if two font data are equal (name, height and style)
	 * 
	 * @param fontTarget
	 * @param fontSource
	 * @return
	 */
	static public boolean isDifferentFontData(FontData fontTarget[], FontData fontSource[]) {
		boolean isChanged = false;
		for (int i=0; i<fontTarget.length; i++) {
			if (i < fontSource.length) {
				FontData source = fontSource[i];
				// bug: if the height is not common, we just do nothing, consider everything work fine
				if (source.getHeight()<4 || source.getHeight()>99)
					return false;
				
				FontData target = fontTarget[i];
				isChanged = !( target.getName().equals(source.getName()) &&
						(target.getHeight()==source.getHeight()) && 
						(target.getStyle()==source.getStyle()) ) ;
				
				if (isChanged)
					// no need to continue the loop
					return isChanged;
			}
		}
		return isChanged;
	}
	
	/****
	 * remove all the allocated resources
	 */
	static public void dispose() {
		try {
			Utilities.fontGeneral.dispose();
			Utilities.fontMetric.dispose();
			COLOR_TOP.dispose();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/****
	 * Store the new fonts into the workspace registry
	 */
	static private void storePreferenceFonts() {
/*		ScopedPreferenceStore objPref = (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
		PreferenceConverter.setValue(objPref, PreferenceConstants.P_FONT_GENERIC, Utilities.fontGeneral.getFontData());
		PreferenceConverter.setValue(objPref, PreferenceConstants.P_FONT_METRIC, Utilities.fontMetric.getFontData());*/
	}
	
	/**
	 * Refresh all the views 
	 * @param window: the target window
	 */
	static private void resetAllViews(IWorkbenchWindow window) {

	}


	
	
	/**
	 * refresh size of rows for a particular view - non Windows
	 * @param tree
	 */
	static public void resetViewRowHeight ( TreeViewer tree ) {
		if (!OSValidator.isWindows()) { 
			int saveWidth = tree.getTree().getColumn(0).getWidth();
			tree.getTree().getColumn(0).setWidth(saveWidth==0?1:0);
			tree.getTree().getColumn(0).setWidth(saveWidth);
		}
	}
	
	/**
	 * Update the font for metric pane with one single font (just take the size)
	 * @param objFontData
	 */
	static private void setFontMetric(IWorkbenchWindow window, int iFontSize) {
			FontData []myFontGeneric = Utilities.fontGeneral.getFontData();
			int iSize = myFontGeneric[0].getHeight() + iFontSize;
			myFontGeneric[0].setHeight(iSize);
			
			FontData []myFontMetric = Utilities.fontMetric.getFontData();
			iSize = myFontMetric[0].getHeight() + iFontSize;
			myFontMetric[0].setHeight(iSize);
			
			setFontMetric(window, myFontMetric, myFontGeneric);
			
	}
	
	/**
	 * Increment font size
	 * @param window
	 */
	static public void increaseFont(IWorkbenchWindow window) {
		Utilities.setFontMetric(window, +1);
	}

	
	/**
	 * Decrement font size
	 * @param window
	 */
	static public void DecreaseFont(IWorkbenchWindow window) {
		Utilities.setFontMetric(window, -1);
	}

	/**	
	 * Insert an item on the top on the tree/table with additional image if not null
	 * @param treeViewer : the tree viewer
	 * @param imgScope : the icon for the tree node
	 * @param arrText : the label of the items (started from  col 0..n-1)
	 */
	static public void insertTopRow(TreeViewer treeViewer, Image imgScope, String []arrText) {
		if(arrText == null)
			return;
    	TreeItem item = new TreeItem(treeViewer.getTree(), SWT.BOLD, 0);
    	if(imgScope != null)
    		item.setImage(0,imgScope);

    	// Laksono 2009.03.09: add background for the top row to distinguish with other scopes
    	item.setBackground(Utilities.COLOR_TOP);
    	// make monospace font for all metric columns
    	item.setFont(Utilities.fontMetric);
    	item.setFont(0, Utilities.fontGeneral); // The tree has the original font
    	// put the text on the table
    	item.setText(arrText);
    	// set the array of text as the item data 
    	// we will use this information when the table is sorted (to restore the original top row)
    	item.setData(arrText);
	}

	/**
	 * Retrieve the top row items into a list of string
	 * @param treeViewer
	 * @return
	 */
	public static String[] getTopRowItems( TreeViewer treeViewer ) {
		TreeItem item = treeViewer.getTree().getItem(0);
		String []sText= null; // have to do this to avoid error in compilation;
		if(item.getData() instanceof Scope) {
			// the table has been zoomed-out
		} else {
			// the table is in original form or flattened or zoom-in
			Object o = item.getData();
			if(o != null) {
				Object []arrObj = (Object []) o;
				if(arrObj[0] instanceof String) {
					sText = (String[]) item.getData(); 
				}
			}
		}
		return sText;
	}
	/**
	 * Return an image depending on the scope of the node.
	 * The criteria is based on ScopeTreeCellRenderer.getScopeNavButton()
	 * @param scope
	 * @return
	 */
	static public Image getScopeNavButton(Scope scope) {
		if (scope instanceof CallSiteScope) {
			CallSiteScope scopeCall = (CallSiteScope) scope;
        	LineScope lineScope = (scopeCall).getLineScope();
			if (((CallSiteScope) scope).getType() == CallSiteScopeType.CALL_TO_PROCEDURE) {
				if(Utilities.isFileReadable(lineScope))
					return getResource().getImage(ResourceConstant.IMG_NODE_CALLTO);
				else
					return getResource().getImage(ResourceConstant.IMG_NODE_CALLTO_DISABLED);
			} else {
				if(Utilities.isFileReadable(lineScope))
					return getResource().getImage(ResourceConstant.IMG_NODE_CALLFROM);
				else
					return getResource().getImage(ResourceConstant.IMG_NODE_CALLFROM_DISABLED);
			}
		}
		return null;
	}
	
	
	static private ResourceProvider getResource()
	{
		if (resource == null) {
			resource = new ResourceProvider();
		}
		return resource;
	}
	

    /**
     * Verify if the file exist or not.
     * Remark: we will update the flag that indicates the availability of the source code
     * in the scope level. The reason is that it is less time consuming (apparently) to
     * access to the scope level instead of converting and checking into FileSystemSourceFile
     * level.
     * @param scope
     * @return true if the source is available. false otherwise
     */
    static public boolean isFileReadable(Scope scope) {
    	// check if the source code availability is already computed
    	if(scope.iSourceCodeAvailability == Scope.SOURCE_CODE_UNKNOWN) {
    		SourceFile newFile = (scope.getSourceFile());
    		if (newFile != null && !newFile.getName().isEmpty()) {
        		if( (newFile != SourceFile.NONE)
            			|| ( newFile.isAvailable() )  ) {
            			if (newFile instanceof FileSystemSourceFile) {
            				FileSystemSourceFile objFile = (FileSystemSourceFile) newFile;
            				if(objFile != null) {
            					// find the availability of the source code
            					if (objFile.isAvailable()) {
            						scope.iSourceCodeAvailability = Scope.SOURCE_CODE_AVAILABLE;
            						return true;
            					} 
            				}
            			}
            		}
    		}
    	} else
    		// the source code availability is already computed, we just reuse it
    		return (scope.iSourceCodeAvailability == Scope.SOURCE_CODE_AVAILABLE);
    	// in this level, we don't think the source code is available
		scope.iSourceCodeAvailability = Scope.SOURCE_CODE_NOT_AVAILABLE;
		return false;
    }
}
