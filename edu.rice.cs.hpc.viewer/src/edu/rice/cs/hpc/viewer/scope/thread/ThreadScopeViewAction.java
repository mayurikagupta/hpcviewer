package edu.rice.cs.hpc.viewer.scope.thread;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.scope.AbstractBaseScopeView;
import edu.rice.cs.hpc.viewer.scope.BaseScopeViewActions;
import edu.rice.cs.hpc.viewer.scope.ScopeViewActions;
import edu.rice.cs.hpc.viewer.scope.topdown.CallingContextActionsGUI;

public class ThreadScopeViewAction extends BaseScopeViewActions 
{
	private IMetricManager manager;
	
	public ThreadScopeViewAction(AbstractBaseScopeView view, IWorkbenchWindow window,
			Composite parent, CoolBar coolbar, IMetricManager metricManager) {
		super(window, view, parent, coolbar, ScopeViewActions.ActionScope.THIS_VIEW_ONLY);
		this.manager = metricManager;
	}

	public void setMetricManager(IMetricManager manager)
	{
		this.manager = manager;
	}
	
	@Override
	public void checkStates(Scope nodeSelected)
	{
		super.checkStates(nodeSelected);
		((CallingContextActionsGUI)objActionsGUI).checkStates(nodeSelected);
	}

	@Override
	protected  Composite createGUI(Composite parent, CoolBar coolbar) {
    	objActionsGUI = new CallingContextActionsGUI(this.objShell, 
    			this.objWindow, parent, this, false);
    	
    	return objActionsGUI.buildGUI(parent, coolbar);
	}
	
	@Override
	protected IMetricManager getMetricManager() {
		return manager;
	}
}
