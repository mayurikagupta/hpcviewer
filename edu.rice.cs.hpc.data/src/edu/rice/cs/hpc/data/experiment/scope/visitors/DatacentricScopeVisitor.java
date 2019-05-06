package edu.rice.cs.hpc.data.experiment.scope.visitors;

import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.FileScope;
import edu.rice.cs.hpc.data.experiment.scope.GroupScope;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.LoadModuleScope;
import edu.rice.cs.hpc.data.experiment.scope.LoopScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.ScopeVisitType;
import edu.rice.cs.hpc.data.experiment.scope.StatementRangeScope;
import edu.rice.cs.hpc.data.util.Constants;


public class DatacentricScopeVisitor implements IScopeVisitor 
{
	enum Status {
		ROOT, 
		ALLOCATION_PATH, 
		MEMACCESS_CALLPATH,
		STATIC_CALLPATH,
		UNKNOWN_CALLPATH};
		
	Status status;

	public DatacentricScopeVisitor() {
		this.status = Status.ROOT;
	}
	
	@Override
	public void visit(LineScope scope, ScopeVisitType vt) {
		updateStatus(scope, vt);
	}

	@Override
	public void visit(StatementRangeScope scope, ScopeVisitType vt) {}

	@Override
	public void visit(LoopScope scope, ScopeVisitType vt) {
		updateStatus(scope, vt);
	}

	@Override
	public void visit(CallSiteScope scope, ScopeVisitType vt) {
		
		ProcedureScope proc = scope.getProcedureScope();
		checkScope(scope, proc, vt);
	}

	@Override
	public void visit(ProcedureScope scope, ScopeVisitType vt) {
		
		checkScope(scope, scope, vt);
	}

	@Override
	public void visit(FileScope scope, ScopeVisitType vt) {}

	@Override
	public void visit(GroupScope scope, ScopeVisitType vt) {}

	@Override
	public void visit(LoadModuleScope scope, ScopeVisitType vt) {}

	@Override
	public void visit(RootScope scope, ScopeVisitType vt) {}

	@Override
	public void visit(Scope scope, ScopeVisitType vt) {}

	
	private void checkScope(Scope scope, ProcedureScope proc, ScopeVisitType vt) {
		
		if (vt == ScopeVisitType.PreVisit) { 
			if (proc.getNodeType()== Constants.NODE_TYPE.NODE_TYPE_ALLOCATION) {
			
				scope.incrementCounter();
				status = Status.MEMACCESS_CALLPATH;
			} else {
				updateStatus(scope, vt);
			}
		} else if (vt == ScopeVisitType.PostVisit) {
			if (proc.getNodeType() == Constants.NODE_TYPE.NODE_TYPE_ALLOCATION) {
				
				scope.decrementCounter();
				status = Status.ROOT;
			}
		}
	}
	
	private void updateStatus(Scope scope, ScopeVisitType vt) {
		if (vt == ScopeVisitType.PreVisit) {
			if (status == Status.MEMACCESS_CALLPATH) {
				scope.incrementCounter();
			}
		}
	}
}
