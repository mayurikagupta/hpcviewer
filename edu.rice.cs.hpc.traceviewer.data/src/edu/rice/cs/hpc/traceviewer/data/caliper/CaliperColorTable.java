package edu.rice.cs.hpc.traceviewer.data.caliper;

import java.util.HashMap;
import java.util.Random;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractColorTable;

public class CaliperColorTable extends AbstractColorTable {

	public CaliperColorTable(IWorkbenchWindow window) {
		super(window);
	}
	
	/**
	 * Returns the color that corresponds to the name's class
	 */
	public Color getColor(Object key)
	{
		String name = renderName((String)key);
		return super.getColor(name);
	}
	
	/**
	 * returns the image that corresponds to the name's class
	 * @param name
	 * @return
	 */
	public Image getImage(Object key) 
	{
		String name = renderName((String)key);
		return super.getImage(name);
	}
	
	private String renderName(String name) {
		/* The name is the display name. As a result, we need to render the display name 
		 * to get the color name when necessary.
		 */
		if (name.contains(CaliperUtils.ITERATION_AT)) 
			name = name.substring(name.indexOf(CaliperUtils.ITERATION_AT) 
					+ CaliperUtils.ITERATION_AT.length());
		return name;
	}
	
	/*********************************************************************
	 * Fills the colorMatcher with unique "random" colors that correspond
	 * to each caliper stack frame's name in names.
	 *********************************************************************/
	public void setColorTable()
	{	
		if (colorMatcher != null)
			dispose();
		
		//This is where the data file is converted to the colorTable using colorMatcher.
		//creates name-function-color colorMatcher for each function.
		colorMatcher = new HashMap<Object,ColorImagePair>();
		{
			// rework the color assignment to use a single random number stream
			Random r = new Random((long)612543231);
			int cmin = 16;
			int cmax = 200 - cmin;
			for (int l=0; l<keys.size(); l++) {
				
				String name = (String) keys.get(l);
				
				if (!colorMatcher.containsKey(name)) {
						
						RGB rgb = getRandomColor( name, cmin, cmax, r );
						Color c = new Color(display, rgb);
						colorMatcher.put(name, new ColorImagePair(c));
					}
			}
		}
	}
	
	/***********************************************************************
	 * retrieve color for a procedure. If the procedure has been assigned to
	 * 	a color, we'll return the allocated color, otherwise, create a new one
	 * 	randomly.
	 ***********************************************************************/
	private RGB getRandomColor( String name, int colorMin, int colorMax, Random r ) {
		return new RGB(	colorMin + r.nextInt(colorMax), 
				colorMin + r.nextInt(colorMax), 
				colorMin + r.nextInt(colorMax));
	}
}
