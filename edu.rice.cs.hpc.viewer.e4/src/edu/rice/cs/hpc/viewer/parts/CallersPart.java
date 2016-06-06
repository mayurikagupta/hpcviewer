 
package edu.rice.cs.hpc.viewer.parts;

import javax.inject.Inject;
import javax.annotation.PostConstruct;
import org.eclipse.swt.widgets.Composite;

public class CallersPart {
	@Inject
	public CallersPart() {
		
	}
	
	@PostConstruct
	public void postConstruct(Composite parent) {
		System.out.println("CV");
	}
	
	
	
	
}