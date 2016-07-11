package edu.rice.cs.hpc.traceviewer.data.graph;

import java.util.Vector;

import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractStack;

public class CallPath extends AbstractStack
{

	/**the Scope at the current cpid*/
	private Scope leafScope;
	
	public CallPath(Scope _leafScope, int _maxDepth, Scope _currentDepthScope, int _currentDepth)
	{
		super(_maxDepth);
		leafScope = _leafScope;
	}
	
	public CallPath(Scope _leafScope, int _maxDepth)
	{
		this(_leafScope, _maxDepth, null, _maxDepth);
	}
	
	@Override
	public String getColorNameAt(int depth) {
		return this.getScopeAt(depth).getName();
	}
	
	/**returns the scope at the given depth that's along the path between the root scope and the leafScope*/
	public Scope getScopeAt(int depth)
	{
		if (depth < 0)
			return null;
		
		int cDepth = maxDepth;
		Scope cDepthScope = leafScope;

		while(!(cDepthScope.getParentScope() instanceof RootScope) && 
				(cDepth > depth || !(cDepthScope instanceof CallSiteScope || cDepthScope instanceof ProcedureScope)))
		{
			cDepthScope = cDepthScope.getParentScope();
			if((cDepthScope instanceof CallSiteScope) || (cDepthScope instanceof ProcedureScope))
				cDepth--;
		}
		
		assert (cDepthScope instanceof CallSiteScope || cDepthScope instanceof ProcedureScope);

		return cDepthScope;
	}
	
	
	/*************************************
	 * retrieve the list of function names of this call path
	 * 
	 * @return vector of procedure names
	 ************************************/
	public Vector<String> getDisplayNames()
	{
		final Vector<String> functionNames = new Vector<String>();
		if (functionNames.isEmpty())
		{
			Scope currentScope = leafScope;
			int depth = maxDepth;
			while(depth > 0)
			{
				if ((currentScope instanceof CallSiteScope) || (currentScope instanceof ProcedureScope))
				{
					functionNames.add(0, currentScope.getName());
					depth--;
				}
				currentScope = currentScope.getParentScope();
			}
		}
		return functionNames;
	}

	@Override
	public boolean isSameInstanceAtDepth(AbstractStack other, int depth) {
		return this.getColorNameAt(depth).equals(other.getColorNameAt(depth));
	}
}