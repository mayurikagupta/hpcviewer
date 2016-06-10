 
package edu.rice.cs.hpc.viewer.action;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.e4.core.di.annotations.CanExecute;

public class DatabaseOpen {
	@Execute
	public void execute(Shell shell) {
		System.out.println(shell.getText());
	}
	
	
	@CanExecute
	public boolean canExecute() {
		
		return true;
	}
		
}