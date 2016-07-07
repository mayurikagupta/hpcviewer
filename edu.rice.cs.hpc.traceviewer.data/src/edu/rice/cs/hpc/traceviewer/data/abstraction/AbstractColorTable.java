package edu.rice.cs.hpc.traceviewer.data.abstraction;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Provides the color for different names.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Added this abstraction layer so that hpctraceviewer can display data from multiple sources.
 */
public abstract class AbstractColorTable {
	static final public int COLOR_ICON_SIZE = 8;
	
	// data members
	protected HashMap<String, ColorImagePair> colorMatcher;
	
	/**All of the names stored in this colorTable.*/
	protected ArrayList<String> names;
	
	/**The display this ColorTable uses to generate the random colors.*/
	protected Display display;
	
	public AbstractColorTable(IWorkbenchWindow window) {
		names = new ArrayList<String>();
		display = window.getShell().getDisplay();
	}
	
	public abstract void setColorTable();
	
	public void dispose() {
		for (ColorImagePair pair: colorMatcher.values()) {
			pair.dispose();
		}
	}
	
	/**
	 * Returns the color in the colorMatcher that corresponds to the name's class
	 * @param name
	 * @return
	 */
	public Color getColor(String name)
	{
		return colorMatcher.get(name).getColor();
	}
	
	/**
	 * returns the image that corresponds to the name's class
	 * @param name
	 * @return
	 */
	public Image getImage(String name) 
	{
		final ColorImagePair cipair = colorMatcher.get(name);
		if (cipair != null) {
			return cipair.getImage();
		} else {
			return null;
		}
	}

	/***
	 * set the name with a new color
	 * @param name
	 * @param color
	 */
	public void setColor(String name, RGB rgb) {
		// dispose old value
		final ColorImagePair oldValue = colorMatcher.get(name);
		if (oldValue != null) {
			oldValue.dispose();
		}
		// create new value
		final ColorImagePair newValue = new ColorImagePair(new Color(display,rgb));
		colorMatcher.put(name, newValue);
	}
	
	/************************************************************************
	 * Adds a name to the list of names in this ColorTable.
	 * NOTE: Doesn't create a color for this name. All the color creating
	 * is done in setColorTable.
	 * @param name The name to be added.
	 ************************************************************************/
	public void addName(String name)
	{
		if(!names.contains(name))
			names.add(name);
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
	
	/************************************************************************
	 * class to pair color and image
	 * @author laksonoadhianto
	 *
	 ************************************************************************/
	protected class ColorImagePair {
		private Color color;
		private Image image;
		
		/****
		 * create a color-image pair
		 * @param color c
		 */
		public ColorImagePair(Color c) {
			// create an empty image filled with color c
			image = AbstractColorTable.createImage(display, c.getRGB());
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
