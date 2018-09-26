package edu.rice.cs.hpc.traceviewer.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for coloring by ID
 * 
 * @author Lai Wei
 *
 */
public class OptionColorByID extends AbstractHandler {

	final static public String commandId = "edu.rice.cs.hpc.traceviewer.colorByID";
	
	public Object execute(ExecutionEvent event) throws ExecutionException 
	{
	     Command command = event.getCommand();
	     HandlerUtil.toggleCommandState(command);
	     // use the old value and perform the operation 
	     return null;
	}

}
