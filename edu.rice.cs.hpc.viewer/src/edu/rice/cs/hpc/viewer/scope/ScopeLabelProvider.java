package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ColumnLabelProvider;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScopeCallerView;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.util.Utilities;
import edu.rice.cs.hpc.viewer.window.ViewerWindow;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;


public class ScopeLabelProvider extends ColumnLabelProvider implements IStyledLabelProvider 
{
	final private ViewerWindow viewerWindow;
	
	public ScopeLabelProvider(IWorkbenchWindow window) {
		viewerWindow = ViewerWindowManager.getViewerWindow(window);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public StyledString getStyledText(Object element) {
		Scope node = (Scope) element;
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
			
			// show the line number
			if (line>0) {
				if (isReadable)
					styledString.append(String.valueOf(line)+": ", Utilities.STYLE_COUNTER);
				else 
					styledString.append(String.valueOf(line)+": ", Utilities.STYLE_DECORATIONS);
			}
		}
		if(Utilities.isFileReadable(node)) {
			styledString.append( text, Utilities.STYLE_ACTIVE_LINK );
		} else {
			styledString.append( text, Utilities.STYLE_INACTIVE_LINK );
		}
		return styledString;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof CallSiteScope) {
			Scope node = (Scope) element;
			final Image image = Utilities.getScopeNavButton(node);
			
			return image;
		}
		return null;
	}

	
	/**
	 * Return the text of the scope tree. By default is the scope name.
	 */
	private String getText(Scope node) 
	{
		String text = "";
			
		if (viewerWindow.showCCTLabel())  
		{
			//---------------------------------------------------------------
			// label for debugging purpose
			//---------------------------------------------------------------
			if (node instanceof CallSiteScope)
			{
				CallSiteScope caller = (CallSiteScope) node;
				Scope cct = caller.getLineScope();
				if (node instanceof CallSiteScopeCallerView) 
				{
					Object merged[] = ((CallSiteScopeCallerView)caller).getMergedScopes();
					if (merged != null) {
						int mult = merged.length + 1;
						text = mult + "*";
					}
					cct = ((CallSiteScopeCallerView)caller).getScopeCCT();
				}	
				text += "[c:" + caller.getCCTIndex() +"/" + cct.getCCTIndex()  + "] " ;
			} else
				text = "[c:" + node.getCCTIndex() + "] ";
		} 
		if (viewerWindow.showFlatLabel()) {
			text += "[f: " + node.getFlatIndex() ;
			if (node instanceof CallSiteScope) {
				ProcedureScope proc = ((CallSiteScope)node).getProcedureScope();
				text += "/" + proc.getFlatIndex();
			}
			text += "] ";
		} 
		text += node.getName();	
		return text;
	}
}
