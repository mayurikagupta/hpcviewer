package edu.rice.cs.hpc.viewer.scope.datacentric;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.visitors.DatacentricScopeVisitor;
import edu.rice.cs.hpc.viewer.graph.GraphMenu;
import edu.rice.cs.hpc.viewer.scope.AbstractContentProvider;
import edu.rice.cs.hpc.viewer.scope.BaseScopeView;
import edu.rice.cs.hpc.viewer.scope.ScopeLabelProvider;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;
import edu.rice.cs.hpc.viewer.scope.ScopeViewActions;
import edu.rice.cs.hpc.viewer.scope.topdown.CallingContextViewActions;
import edu.rice.cs.hpc.viewer.util.Utilities;

public class DataView extends BaseScopeView 
{
	final static public String ID = "edu.rice.cs.hpc.viewer.scope.datacentric.DataView";
	
	private DataViewContentProvider  contentProvider = null;
	private DataViewLabelProvider labelProvider   = null;

	@Override
	protected void updateDatabase(Experiment new_database) {
		RootScope root = new_database.getDatacentricRootScope();
		DatacentricScopeVisitor visitor = new DatacentricScopeVisitor();
		
		root.dfsVisitScopeTree(visitor);
	}

	@Override
	protected void refreshTree(RootScope root) {
		updateDisplay();
	}

	@Override
	protected ScopeViewActions createActions(Composite parent, CoolBar coolbar) {
		IWorkbenchWindow window = this.getSite().getWorkbenchWindow();		
		
    	ScopeViewActions action = new CallingContextViewActions(
    			getViewSite().getShell(), window, parent, coolbar);
    	
    	return action;
	}

	@Override
	protected void mouseDownEvent(Event event) {}

	@Override
	protected void createAdditionalContextMenu(IMenuManager mgr, Scope scope) {
		IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
		GraphMenu.createAdditionalContextMenu(window, mgr, database, scope);
	}

	@Override
	protected AbstractContentProvider getScopeContentProvider() {
		if (contentProvider == null) {
			contentProvider = new DataViewContentProvider(getTreeViewer());
		}
		return contentProvider;
	}

	@Override
	protected CellLabelProvider getLabelProvider() {
		if (labelProvider == null) {
			labelProvider = new DataViewLabelProvider(this.getSite().getWorkbenchWindow()); 
		}
		return labelProvider;
	}
	
	/****************************************************************
	 * 
	 * Datacentric content provider
	 *
	 ****************************************************************/
	static class DataViewContentProvider extends AbstractContentProvider
	{

		public DataViewContentProvider(ScopeTreeViewer viewer) {
			super(viewer);
		}

		@Override
		public Object[] getChildren(Object node) {
			if (node instanceof Scope) {
				return ((Scope)node).getChildren();
			}
			return null;
		}
	};
	
	private static class DataViewLabelProvider extends DelegatingStyledCellLabelProvider
	{

		public DataViewLabelProvider(IWorkbenchWindow window) {
			super(new DataViewStyledLabelProvider(window));
		}
		
	}
	
	private static class DataViewStyledLabelProvider extends ScopeLabelProvider
	{

		DataViewStyledLabelProvider(IWorkbenchWindow window) {
			super(window);
			// TODO Auto-generated constructor stub
		}
	
		
		@Override
		public StyledString getStyledText(Object element) {
			final Scope node  = (Scope) element;
			
			final String text = getText(node);
			
			StyledString styledString= new StyledString();
			
			// ----------------------------------------------
			// special case for call sites :
			// - coloring the object for call site (if exists)
			// - show the icon if exists
			// ----------------------------------------------
			if (element instanceof CallSiteScope) {
				final CallSiteScope cs = (CallSiteScope) element;
				
				// the line number in XML is 0-based, while the editor is 1-based
				int line = 1+cs.getLineScope().getFirstLineNumber();
				boolean isReadable = Utilities.isFileReadable(cs.getLineScope());
				
				if (line > 0) {
					// show the line number
					if (isReadable)
						styledString.append(String.valueOf(line)+": ", Utilities.STYLE_COUNTER);
					else 
						styledString.append(String.valueOf(line)+": ", Utilities.STYLE_DECORATIONS);
				}
			}
			if(Utilities.isFileReadable(node)) {
				if (node.isCounterPositif())
					styledString.append( text, Utilities.STYLE_DATACENTRIC_MEMACCESS_ACTIVE );
				else
					styledString.append( text, Utilities.STYLE_ACTIVE_LINK );
			} else {
				if (node.isCounterPositif())
					styledString.append( text, Utilities.STYLE_DATACENTRIC_MEMACCESS_INACTIVE );
				else
					styledString.append( text, Utilities.STYLE_INACTIVE_LINK );
			}
			return styledString;
		}
	}
}

