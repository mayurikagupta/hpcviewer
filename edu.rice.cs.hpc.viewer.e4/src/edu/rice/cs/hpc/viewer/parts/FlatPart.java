 
package edu.rice.cs.hpc.viewer.parts;

import javax.inject.Inject;
import javax.annotation.PostConstruct;
import org.eclipse.swt.widgets.Composite;

public class FlatPart {
	@Inject
	public FlatPart() {
		
	}
	
	@PostConstruct
	public void postConstruct(Composite parent) {
		System.out.println("FV");
	}
	
	
	
	
}