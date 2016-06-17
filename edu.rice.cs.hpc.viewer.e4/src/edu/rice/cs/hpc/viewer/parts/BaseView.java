package edu.rice.cs.hpc.viewer.parts;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.viewer.action.BaseActionToolBar;
import edu.rice.cs.hpc.viewer.components.ScopeTreeViewer;
import edu.rice.cs.hpc.viewer.util.Utilities;

public abstract class BaseView {

	@Inject
	public BaseView() {
	}

	protected ColumnViewerSorter sorterTreeColumn;	// sorter for the tree
	private ScopeTreeViewer treeViewer;
	
	
	@PostConstruct
	public void postConstruct(Composite parent) {
		
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);

		BaseActionToolBar action = getActionToolBar();
		action.create(parent);
		
		treeViewer = new ScopeTreeViewer(parent, SWT.BORDER|SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		treeViewer.setContentProvider( getContentProvider() );
		final Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
        //-----------------
        // Laks 11.11.07: need this to expand the tree for all view
        GridData data = new GridData(GridData.FILL_BOTH);
        treeViewer.getTree().setLayoutData(data);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tree);
	}

	
	/***
	 * set input database
	 * This method will fill the table with the root scope of the database
	 * if the view is cct, it will show cct root
	 * 
	 * @param experiment
	 */
	public void setDatabase(Experiment experiment) {

        final int numMetric			  = experiment.getMetricCount();

        //----------------- create the column tree
        TreeViewerColumn colTree;		// column for the scope tree
        colTree = new TreeViewerColumn(treeViewer,SWT.LEFT, 0);
        colTree.getColumn().setText("Scope");
        colTree.getColumn().setWidth(200); //TODO dynamic size
        colTree.setLabelProvider( new StyledScopeLabelProvider() ); // laks addendum
        sorterTreeColumn = new ColumnViewerSorter(treeViewer, colTree.getColumn(), null,0); 
        
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
            		colMetrics[i] = treeViewer.addTreeColumn(metric, (i==0));
        		}
        	}
            //treeViewer.setColumnProperties(titles); // do we need this ??
        }

        // fill the table
        RootScope root = getRoot(experiment);
        if (root != null) {
        	treeViewer.setInput(root);
        	insertTopLine(root);
        }
	}
	
	@PreDestroy
	public void preDestroy() {		
	}
	
	
	@Focus
	public void onFocus() {
	}
	
	private void insertTopLine(RootScope root)
	{
    	// Bug fix: avoid using list of columns from the experiment
    	// formerly: .. = this.myExperiment.getMetricCount() + 1;
    	TreeColumn []columns = treeViewer.getTree().getColumns();
    	int nbColumns = columns.length; 	// columns in base metrics
    	String []sText = new String[nbColumns];
    	sText[0] = new String(root.getName());
    	
    	// --- prepare text for base metrics
    	// get the metrics for all columns
    	for (int i=1; i< nbColumns; i++) {
    		// we assume the column is not null
    		Object o = columns[i].getData();
    		if(o instanceof BaseMetric) {
    			BaseMetric metric = (BaseMetric) o;
    			// ask the metric for the value of this scope
    			// if it's a thread-level metric, we will read metric-db file
    			sText[i] = metric.getMetricTextValue(root);
    		}
    	}
    	
    	// draw the root node item
    	Utilities.insertTopRow(treeViewer, Utilities.getScopeNavButton(root), sText);

	}
	
	abstract protected BaseActionToolBar getActionToolBar();
	abstract protected ScopeTreeContentProvider getContentProvider();
	abstract protected RootScope getRoot(Experiment experiment);
}
