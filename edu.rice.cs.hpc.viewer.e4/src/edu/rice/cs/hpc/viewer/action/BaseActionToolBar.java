package edu.rice.cs.hpc.viewer.action;

import java.util.HashMap;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import edu.rice.cs.hpc.viewer.resources.ResourceConstant;
import edu.rice.cs.hpc.viewer.resources.ResourceProvider;


abstract public class BaseActionToolBar 
{
	static public enum ActionID {ZOOM_IN, ZOOM_OUT, HOTPATH, DERIVED_METRIC,
								 CHECK_COLUMNS, SAVE_CSV, FONT_BIGGER, FONT_SMALLER};

	private HashMap<ActionID, ToolItem> titmItems = new HashMap<>(8);

	public Composite create(Composite aParent) {
		
		Composite parent = new Composite(aParent, SWT.NONE);
		
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);

		CoolBar coolBar = new CoolBar(parent, SWT.FLAT);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.LEFT, SWT.CENTER).applyTo(coolBar);

		ToolBar toolBar = new ToolBar(coolBar, SWT.FLAT | SWT.RIGHT);
		
    	ResourceProvider res = new ResourceProvider();
    	
    	// add actions at the beginning
    	addActionBegin(res, toolBar);

    	// default actions
    	createToolItem(res, toolBar, ActionID.ZOOM_IN, ResourceConstant.IMG_TREE_ZOOMIN,
    			"Zoom in to the selected node to be the root");

    	createToolItem(res, toolBar, ActionID.ZOOM_OUT, ResourceConstant.IMG_TREE_ZOOMOUT,
    			"Zoom out: Return to the previous root tree");

    	createToolItem(res, toolBar, ActionID.HOTPATH, ResourceConstant.IMG_TREE_HOTPATH,
    			"Expand the hot path below the selected node");

    	createToolItem(res, toolBar, ActionID.DERIVED_METRIC, ResourceConstant.IMG_ACTION_DERIVED_METRIC,
    			"Add a new derived metric");

    	createToolItem(res, toolBar, ActionID.CHECK_COLUMNS, ResourceConstant.IMG_ACTION_CHECK_COLUMNS,
    			"Hide/show columns");

    	createToolItem(res, toolBar, ActionID.SAVE_CSV, ResourceConstant.IMG_TREE_SAVECSV,
    			"Export the current view into a comma separated value file");

    	new ToolItem(toolBar, SWT.SEPARATOR);
    	
    	createToolItem(res, toolBar, ActionID.FONT_BIGGER, ResourceConstant.IMG_ACTION_FONT_BIGGER,
    			"Increase font size");

    	createToolItem(res, toolBar, ActionID.FONT_SMALLER, ResourceConstant.IMG_ACTION_FONT_SMALLER,
    			"Decrease font size");
    	
    	// add actions at the end
    	addActionEnd(res, toolBar);

    	createCoolItem(coolBar, toolBar);

    	MessageLabel lbl = new MessageLabel(parent);

    	lbl.postConstruct(parent);
    	
    	return coolBar;
	}
	
	
    /**
     * Creating an item for the existing coolbar
     * @param coolBar
     * @param toolBar
     */
	private void createCoolItem(CoolBar coolBar, Control toolBar) {
    	CoolItem coolItem = new CoolItem(coolBar, SWT.NULL);
    	coolItem.setControl(toolBar);
    	org.eclipse.swt.graphics.Point size =
    		toolBar.computeSize( SWT.DEFAULT, SWT.DEFAULT);
    	org.eclipse.swt.graphics.Point coolSize = coolItem.computeSize (size.x, size.y);
    	coolItem.setSize(coolSize);    	
    }

    private void createToolItem(ResourceProvider res, ToolBar toolBar, ActionID ID, String imageID, String tooltip) {
		final ToolItem ti = new ToolItem(toolBar, SWT.PUSH);
		ti.setEnabled(false);
		ti.setToolTipText(tooltip);
		ti.setImage(res.getImage(imageID));
		titmItems.put(ID, ti);
    }
    
    abstract protected void addActionBegin(ResourceProvider res, ToolBar toolBar);
    abstract protected void addActionEnd(ResourceProvider res, ToolBar toolBar);
    
}
