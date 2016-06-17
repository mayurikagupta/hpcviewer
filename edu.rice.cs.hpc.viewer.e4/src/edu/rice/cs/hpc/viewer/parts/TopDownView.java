 
package edu.rice.cs.hpc.viewer.parts;

import org.eclipse.swt.widgets.ToolBar;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.viewer.action.BaseActionToolBar;
import edu.rice.cs.hpc.viewer.resources.ResourceProvider;

public class TopDownView extends BaseView
{

	@Override
	protected BaseActionToolBar getActionToolBar() {
		return new BaseActionToolBar() {

			@Override
			protected void addActionBegin(ResourceProvider res, ToolBar toolBar) {
			}

			@Override
			protected void addActionEnd(ResourceProvider res, ToolBar toolBar) {
			}
		};
	}

	@Override
	protected ScopeTreeContentProvider getContentProvider() {

		return new ScopeTreeContentProvider();
	}

	@Override
	protected RootScope getRoot(Experiment experiment) {
		return experiment.getRootScope(RootScopeType.CallingContextTree);
	}
}