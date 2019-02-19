package edu.rice.cs.hpc.viewer.scope.flat;

import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.scope.AbstractContentProvider;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;

public class FlatViewContentProvider extends AbstractContentProvider {

	public FlatViewContentProvider(ScopeTreeViewer viewer) {
		super(viewer);
	}


    
	@Override
	public Object[] getChildren(Object node) {
		if (node instanceof Scope) {
			return ((Scope)node).getChildren();
		}
		return null;
	}

}
