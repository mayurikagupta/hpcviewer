 
package edu.rice.cs.hpc.viewer.parts;

import javax.inject.Inject;
import javax.annotation.PostConstruct;
import org.eclipse.swt.widgets.Composite;

public class CallingContextPart {
	@Inject
	public CallingContextPart() {
		
	}
	
	@PostConstruct
	public void postConstruct(Composite parent) {
		System.out.println("CCT");
	}
	
	
	
	
}