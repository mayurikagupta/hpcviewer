/**
 * 
 */
package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.TreeItem;

import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.metric.MetricLabelProvider;
import edu.rice.cs.hpc.viewer.util.Utilities;

/**
 * we set lazy virtual bit in this viewer
 */
public class ScopeTreeViewer extends TreeViewer 
{
	final static public String COLUMN_DATA_WIDTH = "w"; 
	final static public int COLUMN_DEFAULT_WIDTH = 120;

	// sort attributes
	private TreeViewerColumn sort_column = null;
	private int sort_direction = ScopeSelectionAdapter.NONE;
	private HashMap<Scope, Object[]> sort_scopes;
	private ScopeComparator comparator;
	
	/**
	 * @param parent
	 */
	public ScopeTreeViewer(Composite parent) {
		super(parent, SWT.VIRTUAL);
		init();
	}

	/**
	 * @param tree
	 */
	public ScopeTreeViewer(Tree tree) {
		super(tree, SWT.VIRTUAL);
		init();
	}

	/**
	 * @param parent
	 * @param style
	 */
	public ScopeTreeViewer(Composite parent, int style) {
		super(parent, SWT.VIRTUAL | style);
		this.setUseHashlookup(true);
		init();
	}

	private void init() 
	{
		getTree().setLinesVisible(true);
	}
	
	/**
	 * Finding the path based on the treeitem information
	 * @param item
	 * @return
	 */
	public TreePath getTreePath(TreeItem item) {
		return super.getTreePathFromItem(item);
	}


    
	/**
	 * Return the canocalized text from the list of elements 
	 * @param sListOfTexts
	 * @param sSeparator
	 * @return
	 */
	public String getTextBasedOnColumnStatus(String []sListOfTexts, String sSeparator, 
			int startColIndex, int startTextIndex) {
		StringBuffer sBuf = new StringBuffer();
		TreeColumn columns[] = this.getTree().getColumns();
		for ( int i=startColIndex; i<columns.length; i++ ) {
			if ( columns[i].getWidth()>0 ) {
				if (sBuf.length()>0)
					sBuf.append(sSeparator);
				sBuf.append( sListOfTexts[i+startTextIndex] );
			}
		}
		return sBuf.toString();
	}
	
	/**
	 * retrieve the title of the columns
	 * @param iStartColIndex
	 * @param sSeparator
	 * @return
	 */
	public String getColumnTitle(int iStartColIndex, String sSeparator) {
		// get the column title first
		TreeColumn columns[] = this.getTree().getColumns();
		String sTitles[] = new String[columns.length];
		for ( int i=0; i<columns.length; i++ ) {
			sTitles[i] = "\"" + columns[i].getText().trim() + "\"";
		}
		// then get the string based on the column status
		return this.getTextBasedOnColumnStatus(sTitles, sSeparator, iStartColIndex, 0);
	}
	
	/****
	 * refresh the title of all metric columns.
	 * <p/>
	 * warning: this method uses linear search to see if they are metric column or not,
	 * 	so the complexity is O(n). 
	 * 
	 */
	public void refreshColumnTitle() {
		
		String []sText = Utilities.getTopRowItems(this);

		TreeColumn columns[] = this.getTree().getColumns();
		boolean need_to_refresh = false;
		
		for( int i=0; i<columns.length; i++ ) {
			
			TreeColumn column = columns[i]; 
			Object obj = column.getData();
			
			if (obj instanceof BaseMetric) {
				final String title = ((BaseMetric)obj).getDisplayName();
				column.setText(title);
				
				// -----------------------------------------------------------------
				// if the column is a derived metric, we need to refresh the table
				// 	even if the derived metric is not modified at all.
				// this solution is not optimal, but it works
				// -----------------------------------------------------------------
				boolean is_derived = (obj instanceof DerivedMetric);
				need_to_refresh |= is_derived;
				if (is_derived) {
					Object objInp = getInput();
					if (objInp instanceof RootScope) {
						DerivedMetric dm = (DerivedMetric) obj;
						String val = dm.getMetricTextValue((RootScope) objInp);

						// change the current value on the top row with the new value
						sText[i] = val;
					}
				}
			}
		}
		if (need_to_refresh) {
			// -----------------------------------------------------------------
			// refresh the table, and insert the top row back to the table
			//	with the new value of the derived metric
			// -----------------------------------------------------------------
			TreeItem item = getTree().getItem(0);
			Image imgItem = item.getImage(0);
			
			refresh();
			
			Utilities.insertTopRow(this, imgItem, sText);
		}
	}
	
    /**
     * Add new tree column for derived metric
     * @param treeViewer
     * @param objMetric
     * @param iPosition
     * @param bSorted
     * @param b: flag to indicate if this column should be displayed or not (default should be true)
     * @return
     */
    public TreeViewerColumn addTreeColumn(BaseMetric objMetric, //int iPosition, 
    		boolean bSorted) {
    	
    	TreeViewerColumn colMetric = new TreeViewerColumn(this,SWT.RIGHT);	// add column
		colMetric.setLabelProvider(new MetricLabelProvider(objMetric /*, Utilities.fontMetric*/) );

		TreeColumn col = colMetric.getColumn();
    	col.setText(objMetric.getDisplayName());	// set the title
    	col.setWidth(COLUMN_DEFAULT_WIDTH); //TODO dynamic size
		// associate the data of this column to the metric since we
		// allowed columns to move (col position is not enough !)
    	col.setData(COLUMN_DATA_WIDTH, COLUMN_DEFAULT_WIDTH);
    	col.setData(objMetric);
		col.setMoveable(true);

		ScopeSelectionAdapter selectionAdapter = new ScopeSelectionAdapter(this, colMetric);
		
		// catch event when the user sort the column on the column header
		col.addSelectionListener(selectionAdapter);
		
		if(bSorted) {
			selectionAdapter.setSorter(ScopeSelectionAdapter.ASC);
		}
		Layout layout = getTree().getParent().getLayout();
		if (layout instanceof TreeColumnLayout) {
			final ColumnPixelData data = new ColumnPixelData(ScopeTreeViewer.COLUMN_DEFAULT_WIDTH, true, false);
			((TreeColumnLayout)layout).setColumnData(colMetric.getColumn(), data);
		}

		return colMetric;
    }
    
    // --------------------
    // sorting features
    // --------------------
    
    /**
     * store the column to be sorted. This will be used by the sorting method
     * @param column
     */
    public void setSortColumn(TreeViewerColumn column) {
    	this.sort_column = column;
    }
    
    /***
     * store the direction of sorting. This will be used by the sorting method
     * @param direction
     */
    public void setSortDirection(int direction) {
    	this.sort_direction = direction;
    }
    
    /***
     * retrieve the sorting column
     * @return
     */
    public TreeViewerColumn getSortColumn() {
    	return sort_column;
    }
    
    /***
     * retrieve the current sorting direction
     * @return
     */
    public int getSortDirection() {
    	return sort_direction;
    }
    
    /***
     * retrieve the sorted children of the parent.
     * This method will dynamically sort the children when needed.
     * If a parent has never sorted its children, it will sort immediately and return.
     * the next time the same parent asks for its sorted children, it returns from the cache.
     * <br>
     * @param parent
     * @param children
     */
    public Object[] getSortScopes(Scope parent) {
    	Object [] children = sort_scopes.get(parent);
    	
    	if (children == null) {
    		ITreeContentProvider provider = (ITreeContentProvider) getContentProvider();
    		children = provider.getChildren(parent);
    		if (children == null)
    			return null;
    		
    		BaseMetric metric = (BaseMetric) sort_column.getColumn().getData();
    		comparator.setMetric(metric);
    		comparator.setDirection(sort_direction);
    		
    		Arrays.sort(children, comparator);
    		sort_scopes.put(parent, children);
    	}
    	return children;
    }
    
    /***
     * Retrieve a child of a parent for a specific sorted index.
     * 
     * @param parent
     * @param index
     * @return
     */
    public Object getSortScope(Scope parent, int index) {
    	if (index < 0)
    		return null;
    	
    	Object []children = getSortScopes(parent);
    	
    	if (children != null && index<children.length)
    		return children[index];
    	
    	return null;
    }
    
    public void sort_start() {
    	if (sort_scopes != null) {
    		sort_scopes.clear();
    	} else {
    		sort_scopes = new HashMap<Scope, Object[]>();
    		comparator = new ScopeComparator();
    	}
    }
    
    public void sort_end() {
    }
    
    /****
     * Regreshes the viewer starting at the given element
     * 
     * @param element
     * @param updateLabels
     */
    public void refreshElement(Object element, boolean updateLabels)
    {
    	super.internalRefresh(element, updateLabels);
    }
    
	/**
	 * Returns the viewer cell at the given widget-relative coordinates, or
	 * <code>null</code> if there is no cell at that location
	 * 
	 * @param point
	 * 		the widget-relative coordinates
	 * @return the cell or <code>null</code> if no cell is found at the given
	 * 	point
	 * 
	 * @since 3.4
	 */
	/*
    public ViewerCell getCell(Point point) {
		ViewerRow row = getViewerRow(point);
		if (row != null) {
			return row.getCell(point);
		}

		return null;
	}*/
}
