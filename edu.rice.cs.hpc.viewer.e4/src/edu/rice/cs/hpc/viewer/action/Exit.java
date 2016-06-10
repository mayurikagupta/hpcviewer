 
package edu.rice.cs.hpc.viewer.action;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;

public class Exit {
	@Execute
	public void execute(IWorkbench workbench) {
		workbench.close();
	}
		
}