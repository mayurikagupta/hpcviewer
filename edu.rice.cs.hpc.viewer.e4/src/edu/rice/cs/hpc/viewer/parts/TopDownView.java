 
package edu.rice.cs.hpc.viewer.parts;

import javax.inject.Inject;
import javax.annotation.PostConstruct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.viewer.action.BaseActionToolBar;
import edu.rice.cs.hpc.viewer.components.ScopeTreeViewer;
import edu.rice.cs.hpc.viewer.resources.ResourceProvider;
import edu.rice.cs.hpc.viewer.parts.StyledScopeLabelProvider;
import edu.rice.cs.hpc.viewer.parts.ColumnViewerSorter;

import javax.annotation.PreDestroy;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewerColumn;

public class TopDownView 
{
	protected ColumnViewerSorter sorterTreeColumn;	// sorter for the tree
	private ScopeTreeViewer treeViewer;
	
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
		
		treeViewer = new ScopeTreeViewer(parent, SWT.BORDER|SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		treeViewer.setContentProvider(new ScopeTreeContentProvider());
		final Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
        //----------------- create the column tree
        TreeViewerColumn colTree;		// column for the scope tree
        colTree = new TreeViewerColumn(treeViewer,SWT.LEFT, 0);
        colTree.getColumn().setText("Scope");
        colTree.getColumn().setWidth(200); //TODO dynamic size
        colTree.setLabelProvider( new StyledScopeLabelProvider() ); // laks addendum
        sorterTreeColumn = new ColumnViewerSorter(this.treeViewer, colTree.getColumn(), null,0); 
        
        //-----------------
        // Laks 11.11.07: need this to expand the tree for all view
        GridData data = new GridData(GridData.FILL_BOTH);
        treeViewer.getTree().setLayoutData(data);

		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tree);
		
		// -------
		
	}
	
	public void setDatabase(Experiment experiment) {
		System.out.println("opening database: " + experiment.getName());
        final int numMetric			  = experiment.getMetricCount();

        // prepare the data for the sorter class for tree
        sorterTreeColumn.setMetric(experiment.getMetric(0));

        // dirty solution to update titles
        TreeViewerColumn []colMetrics = new TreeViewerColumn[numMetric];
        {
            // Update metric title labels
            String[] titles = new String[numMetric+1];
            titles[0] = "Scope";	// unused element. Already defined
            // add table column for each metric
        	for (int i=0; i<numMetric; i++)
        	{
        		final BaseMetric metric = experiment.getMetric(i);
        		if (metric != null) {
            		titles[i+1] = metric.getDisplayName();	// get the title
            		colMetrics[i] = this.treeViewer.addTreeColumn(metric, (i==0));
        		}
        	}
            treeViewer.setColumnProperties(titles); // do we need this ??
        }

        // fill the table
        RootScope root = experiment.getRootScope(RootScopeType.CallingContextTree);
        if (root != null) {
        	treeViewer.setInput(root);
        }
	}
	
	@PreDestroy
	public void preDestroy() {
		
	}
	
	
	@Focus
	public void onFocus() {
		
	}
	
	
}