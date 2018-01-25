package edu.rice.cs.hpc.traceviewer.data.graph;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;

public class CallPathColorTable extends ProcedureColorTable {
	public CallPathColorTable(IWorkbenchWindow window) {
		super(window);
	}

	public Color getColor(Object key)
	{
		if (key instanceof CallPath) {
			CallPath cp = (CallPath) key;
			for (int i = 0; i < cp.getMaxDepth(); i++)
				if (colorMatcher.containsKey(cp.getScopeAt(i).getCCTIndex()))
					return colorMatcher.get(cp.getScopeAt(i).getCCTIndex()).getColor();
			return this.imageGrey.getColor();
		}
		else 
			return this.imageWhite.getColor();//super.getColor(key);
	}
	
	public Image getImage(Object key)
	{
		if (key instanceof CallPath) {
			CallPath cp = (CallPath) key;
			for (int i = 0; i < cp.getMaxDepth(); i++)
				if (colorMatcher.containsKey(cp.getScopeAt(i).getCCTIndex()))
					return colorMatcher.get(cp.getScopeAt(i).getCCTIndex()).getImage();
			return this.imageGrey.getImage();
		}
		else 
			return this.imageWhite.getImage();//super.getImage(key);
	}
}
