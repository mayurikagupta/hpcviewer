//////////////////////////////////////////////////////////////////////////
//																		//
//	LoopScope.java														//
//																		//
//	experiment.scope.LoopScope -- a loop scope							//
//	Last edited: August 10, 2001 at 2:22 pm								//
//																		//
//	(c) Copyright 2001 Rice University. All rights reserved.			//
//																		//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.experiment.scope;


import edu.rice.cs.hpc.data.experiment.scope.visitors.IScopeVisitor;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;




//////////////////////////////////////////////////////////////////////////
//	CLASS LOOP-SCOPE													//
//////////////////////////////////////////////////////////////////////////

 /**
 *
 * A loop scope in an HPCView experiment.
 *
 */


public class LoopScope extends Scope
{

	private final long vma;

//////////////////////////////////////////////////////////////////////////
//	INITIALIZATION														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Creates a LoopScope.
 ************************************************************************/
	
public LoopScope(RootScope root, SourceFile file, int first, int last, int cct_id, int flat_id, long vma)
{
	super(root, file, first, last, cct_id, flat_id);
	this.vma = vma;
}


public Scope duplicate() {
    return new LoopScope(this.root,  this.sourceFile,  
    		this.firstLineNumber,  this.lastLineNumber, getCCTIndex(), this.flat_node_index, this.vma);
}


//////////////////////////////////////////////////////////////////////////
//	SCOPE DISPLAY														//
//////////////////////////////////////////////////////////////////////////

public long getVMA() {
	return this.vma;
}


/*************************************************************************
 *	Returns the user visible name for this scope.
 ************************************************************************/
	
public String getName()
{
	return "loop at " + this.getSourceCitation();
}




/*************************************************************************
 *	Returns the short user visible name for this scope.
 *
 *	This name is only used in tree views where the scope's name appears
 *	in context with its containing scope's name.
 *
 *	Subclasses may override this to implement better short names.
 *
 ************************************************************************/
	
public String getShortName()
{
	return "loop at " + this.getLineNumberCitation();
}


//////////////////////////////////////////////////////////////////////////
//support for visitors													//
//////////////////////////////////////////////////////////////////////////

public void accept(IScopeVisitor visitor, ScopeVisitType vt) {
	visitor.visit(this, vt);
}


}








