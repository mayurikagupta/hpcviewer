package edu.rice.cs.hpc.traceviewer.data.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpc.common.ui.Util;
import edu.rice.cs.hpc.common.util.ProcedureClassData;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.ScopeID;
import edu.rice.cs.hpc.data.util.IProcedureTable;
import edu.rice.cs.hpc.traceviewer.data.util.ProcedureClassMap;

/**************************************************************
 * A data structure designed to hold all the name-color pairs
 * needed for the actual drawing.
 **************************************************************/
public class ColorTable implements IProcedureTable
{
	static final public int COLOR_ICON_SIZE = 8;
	static private ColorImagePair IMAGE_WHITE;
	// data members
	HashMap<String, ColorImagePair> nameColorMatcher;
	
	HashMap<ScopeID, ColorImagePair> idColorMatcher;
	
	/**All of the function names stored in this colorTable.*/
	HashSet<String> procNames;
	
	/**All of the scope IDs stored in this colorTable.*/
	HashSet<ScopeID> scopeIDs;
	
	/**The display this ColorTable uses to generate the random colors.*/
	Display display;
	
	private ProcedureClassMap classMap;
	
	/**Creates a new ColorTable with Display _display.*/
	public ColorTable()
	{
		procNames = new HashSet<String>();
		// Initializes the CSS that represents time values outside of the
		// time-line.
		procNames.add(CallPath.NULL_FUNCTION);
		
		scopeIDs = new HashSet<ScopeID>();
		
		display = Util.getActiveShell().getDisplay();
		
		// create our own white color so we can dispose later, instead of disposing
		//	Eclipse's white color
		final RGB rgb_white = display.getSystemColor(SWT.COLOR_WHITE).getRGB();
		IMAGE_WHITE = new ColorImagePair( new Color(display, rgb_white));
	}
	
	/**
	 * Dispose the allocated resources
	 */
	public void dispose() {
		if (nameColorMatcher != null)
			for (ColorImagePair pair: nameColorMatcher.values()) {
				pair.dispose();
			}
		nameColorMatcher = null;
		
		if (idColorMatcher != null)
			for (ColorImagePair pair: idColorMatcher.values()) {
				pair.dispose();
			}	
		idColorMatcher = null;
		
		IMAGE_WHITE.dispose();
	}
	
	/**
	 * Returns the color in the colorMatcher that corresponds to the name's class
	 * @param name
	 * @return
	 */
	public Color getColorByName(String name)
	{
		return nameColorMatcher.get(name).getColor();
	}

	/**
	 * Returns the color in the colorMatcher that corresponds to the scope id
	 * @return
	 */
	public Color getColorByID(ScopeID id) {
		return idColorMatcher.get(id).getColor();
	}
	
	/**
	 * returns the image that corresponds to the name's class
	 * @param name
	 * @return
	 */
	public Image getImageByName(String name) 
	{
		final ColorImagePair cipair = nameColorMatcher.get(name);
		if (cipair != null) {
			return cipair.getImage();
		} else {
			return null;
		}
	}
	
	/**
	 * returns the image that corresponds to the scope id
	 * @return
	 */
	public Image getImageByID(ScopeID id) {
		final ColorImagePair cipair = idColorMatcher.get(id);
		if (cipair != null) {
			return cipair.getImage();
		} else {
			return null;
		}
	}
	

	/***
	 * set the procedure name with a new color
	 * @param name
	 * @param color
	 */
	public void setColorByName(String name, RGB rgb) {
		// dispose old value
		final ColorImagePair oldValue = nameColorMatcher.get(name);
		if (oldValue != null) {
			oldValue.dispose();
		}
		// create new value
		final ColorImagePair newValue = new ColorImagePair(new Color(display,rgb));
		nameColorMatcher.put(name, newValue);
	}
	
	/*********************************************************************
	 * Fills the colorMatcher with unique "random" colors that correspond
	 * to each function name in procNames.
	 *********************************************************************/
	public void setColorTable()
	{	
		// initialize the procedure-color map
		classMap = new ProcedureClassMap(display);

		if (nameColorMatcher != null || idColorMatcher != null)
			dispose();
		
		//This is where the data file is converted to the colorTable using colorMatcher.
		//creates name-function-color colorMatcher for each function.
		nameColorMatcher = new HashMap<String,ColorImagePair>();
		{
			// rework the color assignment to use a single random number stream
			Random r = new Random((long)612543231);
			int cmin = 16;
			int cmax = 200 - cmin;
			for (String procName : procNames) {
				if (procName != CallPath.NULL_FUNCTION) {
					if (!nameColorMatcher.containsKey(procName)) {
						
						RGB rgb = getProcedureColor( procName, cmin, cmax, r );
						Color c = new Color(display, rgb);
						nameColorMatcher.put(procName, new ColorImagePair(c));
					}
				} else {
					nameColorMatcher.put(procName, IMAGE_WHITE);
				}
			}
		}
		
		idColorMatcher = new HashMap<ScopeID, ColorImagePair>();
		{
			// rework the color assignment to use a single random number stream
			Random r = new Random((long)612543231);
			int cmin = 16;
			int cmax = 200 - cmin;
			for (ScopeID scopeID : scopeIDs)
				if (!idColorMatcher.containsKey(scopeID)) {
					RGB rgb = getScopeIDColor( scopeID, cmin, cmax, r );
					Color c = new Color(display, rgb);
					idColorMatcher.put(scopeID, new ColorImagePair(c));
				}
		}
	}
	
	/************************************************************************
	 * Adds a name to the list of function names in this ColorTable.
	 * NOTE: Doesn't create a color for this name. All the color creating
	 * is done in setColorTable.
	 * @param name The function name to be added.
	 ************************************************************************/
	public void addProcedure(Scope scope)
	{
		procNames.add(scope.getName());
		scopeIDs.add(scope.getScopeID());
	}
	
	
	/***********************************************************************
	 * create an image based on the color
	 * the caller is responsible to free the image
	 * 
	 * @param display
	 * @param color
	 * @return an image (to be freed)
	 ***********************************************************************/
	static public Image createImage(Display display, RGB color) {
		PaletteData palette = new PaletteData(new RGB[] {color} );
		ImageData imgData = new ImageData(COLOR_ICON_SIZE, COLOR_ICON_SIZE, 1, palette);
		Image image = new Image(display, imgData);
		return image;
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
	
	private RGB getScopeIDColor( ScopeID scopeID, int colorMin, int colorMax, Random r) {
		final RGB rgb;
		rgb = new RGB(	colorMin + r.nextInt(colorMax), 
				colorMin + r.nextInt(colorMax), 
				colorMin + r.nextInt(colorMax));
		return rgb;
	}
	
	/************************************************************************
	 * class to pair color and image
	 * @author laksonoadhianto
	 *
	 ************************************************************************/
	private class ColorImagePair {
		private Color color;
		private Image image;
		
		/****
		 * create a color-image pair
		 * @param color c
		 */
		ColorImagePair(Color c) {
			// create an empty image filled with color c
			image = ColorTable.createImage(display, c.getRGB());
			color = c;
		}
		
		/***
		 * get the color 
		 * @return
		 */
		public Color getColor() {
			return this.color;
		}
		
		/***
		 * get the image
		 * @return
		 */
		public Image getImage() {
			return this.image;
		}
		
		public void dispose() {
			this.color.dispose();
			this.image.dispose();
		}
	}
}