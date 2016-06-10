 
package edu.rice.cs.hpc.viewer.action;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;

public class ZoomIn 
{
	@Inject
	ESelectionService selection;
	
	@Execute
	public void execute() {
		Object obj = selection.getSelection();
		System.out.println("selected: "+obj);
	}
	
	
	@CanExecute
	public boolean canExecute() {
		
		return true;
	}

}