 
package edu.rice.cs.hpc.traceviewer.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import edu.rice.cs.hpc.traceviewer.util.Constant;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.di.annotations.CanExecute;

/***
 * 
 * Option whether to use a midpoint renderer or not
 *
 */
public class UseMidpoint 
{
	public final static String ID_HANDLER = "edu.rice.cs.hpc.traceviewer.handler.midpoint";
	public final static String ID_COMMAND = "edu.rice.cs.hpc.traceviewer.command.midpoint";
	
	
	@Execute
	public void execute(MMenuItem menuItem, IEventBroker eventBroker) {

		IEclipsePreferences preference = InstanceScope.INSTANCE.getNode(Constant.PREF_HPCTRACEVIEWER);
		
		if (preference != null) {
			preference.putBoolean(Constant.PREF_MIDPOINT, menuItem.isSelected());
		}
	}
	
	
	@CanExecute
	public boolean canExecute() {
		
		return true;
	}
		
}