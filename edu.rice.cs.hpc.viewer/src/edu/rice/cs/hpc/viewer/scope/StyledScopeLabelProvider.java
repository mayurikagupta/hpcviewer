package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.util.string.StringUtil;


/***
 * 
 * Class to display label on the tree of views
 * 
 * A node of the tree contains three objects: [icon] [callsite] node_label
 * Every object has colors to indicate if they are clickable or not
 * - An object is clickable if they contain further information such as file source code
 * - Otherwise it is not clickable
 * 
 */
public class StyledScopeLabelProvider extends DelegatingStyledCellLabelProvider 
{	
	/**
	 * Initialization of the class: preparing the colors for each object
	 * 
	 * @param window
	 */
	public StyledScopeLabelProvider(IWorkbenchWindow window) {
		super( new ScopeLabelProvider(window));
	}
	
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
	 */
	public String getToolTipText(Object element)
	{
		if (element instanceof Scope) 
		{	
			final int minLengthForToolTip = 50;  
			final int toolTipDesiredLineLength = 80;
			
			String scopeName = ((Scope)element).getName();
			if (scopeName.length() > minLengthForToolTip) {
				return StringUtil.wrapScopeName(scopeName, toolTipDesiredLineLength);
			}
		}
		return null; // no tool tip for this cell
	}
}
