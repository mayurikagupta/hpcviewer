package edu.rice.cs.hpc.traceviewer.data.graph;

import java.util.HashMap;
import java.util.Random;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.common.util.ProcedureClassData;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractStack;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractColorTable;
import edu.rice.cs.hpc.traceviewer.data.util.ProcedureClassMap;

/**************************************************************
 * A data structure designed to hold all the name-color pairs
 * needed for the actual drawing.
 **************************************************************/
public class ProcedureColorTable extends AbstractColorTable
{
	//TODO This could be generalized.
	private ProcedureClassMap classMap;
	
	/**Creates a new ColorTable with Display _display.*/
	public ProcedureColorTable(IWorkbenchWindow window)
	{
		super(window);
		
		// Initializes the CSS that represents time values outside of the
		// time-line.
		names.add(AbstractStack.NULL_NAME);
	}
	
	/*********************************************************************
	 * Fills the colorMatcher with unique "random" colors that correspond
	 * to each function name in names.
	 *********************************************************************/
	public void setColorTable()
	{	
		// initialize the procedure-color map
		classMap = new ProcedureClassMap(display);

		if (colorMatcher != null)
			dispose();
		
		//This is where the data file is converted to the colorTable using colorMatcher.
		//creates name-function-color colorMatcher for each function.
		colorMatcher = new HashMap<String,ColorImagePair>();
		{
			// rework the color assignment to use a single random number stream
			Random r = new Random((long)612543231);
			int cmin = 16;
			int cmax = 200 - cmin;
			for (int l=0; l<names.size(); l++) {
				
				String procName = names.get(l);
				
				if (procName != AbstractStack.NULL_NAME) {
					
					if (!colorMatcher.containsKey(procName)) {
						
						RGB rgb = getProcedureColor( procName, cmin, cmax, r );
						Color c = new Color(display, rgb);
						colorMatcher.put(procName, new ColorImagePair(c));
					}
				} else {
					colorMatcher.put(procName, imageWhite);
				}
			}
		}
	}
	
	/***********************************************************************
	 * retrieve color for a procedure. If the procedure has been assigned to
	 * 	a color, we'll return the allocated color, otherwise, create a new one
	 * 	randomly.
	 * 
	 * @param name
	 * @param colorMin
	 * @param colorMax
	 * @param r
	 * @return
	 ***********************************************************************/
	private RGB getProcedureColor( String name, int colorMin, int colorMax, Random r ) {
		ProcedureClassData value = this.classMap.get(name);
		final RGB rgb;
		if (value != null)
			rgb = value.getRGB();
		else 
			rgb = new RGB(	colorMin + r.nextInt(colorMax), 
							colorMin + r.nextInt(colorMax), 
							colorMin + r.nextInt(colorMax));
		return rgb;
	}
}