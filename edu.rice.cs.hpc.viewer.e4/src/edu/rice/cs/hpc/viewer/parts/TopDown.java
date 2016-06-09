package edu.rice.cs.hpc.viewer.parts;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.jface.viewers.TreeViewer;

public class TopDown {

	public TopDown() {
	}

	/**
	 * Create contents of the view part.
	 */
	@PostConstruct
	public void createControls(Composite parent) {
		
		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		
		TabItem tbtmTopdownTree = new TabItem(tabFolder, SWT.NONE);
		tbtmTopdownTree.setText("Top-down tree");
		
		ViewForm viewForm = new ViewForm(tabFolder, SWT.NONE);
		tbtmTopdownTree.setControl(viewForm);
		
		CoolBar coolBar = new CoolBar(viewForm, SWT.FLAT);
		viewForm.setTopLeft(coolBar);
		
		CoolItem coolItem = new CoolItem(coolBar, SWT.NONE);
		
		ToolBar toolBar = new ToolBar(coolBar, SWT.FLAT | SWT.RIGHT);
		coolItem.setControl(toolBar);
		
		ToolItem tltmZoomin = new ToolItem(toolBar, SWT.NONE);
		tltmZoomin.setEnabled(false);
		tltmZoomin.setToolTipText("Zoom in to the selected node to be the root");
		tltmZoomin.setImage(ResourceManager.getPluginImage("edu.rice.cs.hpc.viewer.e4", "icons/treeZoomIn.gif"));
		
		ToolItem tltmZoomout = new ToolItem(toolBar, SWT.NONE);
		tltmZoomout.setToolTipText("Return to the previous root tree");
		tltmZoomout.setEnabled(false);
		tltmZoomout.setImage(ResourceManager.getPluginImage("edu.rice.cs.hpc.viewer.e4", "icons/treeZoomOut.gif"));
		tltmZoomout.setText("New Item");
		
		ToolItem toolItem = new ToolItem(toolBar, SWT.SEPARATOR);
		
		ToolItem tltmHotpath = new ToolItem(toolBar, SWT.NONE);
		tltmHotpath.setEnabled(false);
		tltmHotpath.setImage(ResourceManager.getPluginImage("edu.rice.cs.hpc.viewer.e4", "icons/treeHotpath.gif"));
		tltmHotpath.setToolTipText("Expand the hot path below the selected node");
		
		TreeViewer treeViewer = new TreeViewer(viewForm, SWT.BORDER);
		Tree tree = treeViewer.getTree();
		viewForm.setContent(tree);
		
		TabItem tbtmBottomUp = new TabItem(tabFolder, SWT.NONE);
		tbtmBottomUp.setText("Bottom-up");
	}

	@PreDestroy
	public void dispose() {
	}

	@Focus
	public void setFocus() {
		// TODO	Set the focus to control
	}

}
