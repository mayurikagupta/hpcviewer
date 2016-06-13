 
package edu.rice.cs.hpc.viewer.parts;

import javax.inject.Inject;
import javax.annotation.PostConstruct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.viewer.action.BaseActionToolBar;
import edu.rice.cs.hpc.viewer.resources.ResourceProvider;

import javax.annotation.PreDestroy;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;

public class TopDownView 
{
	private TreeViewer treeViewer;
	
	@Inject
	public TopDownView() {
		
	}
	
	@PostConstruct
	public void postConstruct(Composite parent) {
		
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);

		BaseActionToolBar action = new BaseActionToolBar() {

			@Override
			protected void addActionBegin(ResourceProvider res, ToolBar toolBar) {
			}

			@Override
			protected void addActionEnd(ResourceProvider res, ToolBar toolBar) {
				
			}
		};
		
		action.create(parent);
		
		treeViewer = new TreeViewer(parent, SWT.BORDER|SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		final Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tree);
		
		// -------
		
	}
	
	public void setDatabase(Experiment experiment) {
		System.out.println("opening database: " + experiment.getName());
	}
	
	@PreDestroy
	public void preDestroy() {
		
	}
	
	
	@Focus
	public void onFocus() {
		
	}
	
	
}