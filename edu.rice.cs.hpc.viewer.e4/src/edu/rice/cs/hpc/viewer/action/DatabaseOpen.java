
package edu.rice.cs.hpc.viewer.action;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.MApplication;

import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.viewer.experiment.ExperimentManager;
import edu.rice.cs.hpc.viewer.parts.TopDownView;

import java.util.List;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;

public class DatabaseOpen 
{
	public static final String ID_HANDLER = "edu.rice.cs.hpc.viewer.handler.OpenDatabase";
	public static final String ID_COMMAND = "edu.rice.cs.hpc.viewer.command.OpenDatabase";
	
	@Execute
	public void execute(Shell shell, MApplication application, EPartService partService, EModelService modelService) {
		final ExperimentManager manager = new ExperimentManager(shell);
		boolean result = manager.openFileExperiment(ExperimentManager.FLAG_DEFAULT);

		if (result) {
			MPart mpart = partService.createPart("edu.rice.cs.hpc.viewer.partdescriptor.scopeview");

			List<MPartStack> stacks = modelService.findElements(application, null,
					MPartStack.class, null);
			if (stacks != null)
				stacks.get(stacks.size()-1).getChildren().add(mpart);

			mpart.setLabel("Calling-context view");

			partService.showPart(mpart, PartState.ACTIVATE);

			TopDownView tdView = (TopDownView) mpart.getObject();
			tdView.setDatabase(manager.getExperiment());
		}
	}


	@CanExecute
	public boolean canExecute() {

		return true;
	}

}