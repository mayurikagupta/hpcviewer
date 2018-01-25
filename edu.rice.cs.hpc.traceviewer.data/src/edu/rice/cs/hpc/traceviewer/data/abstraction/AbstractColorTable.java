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
 * Provides the color for different keys.
 * 
 * @log
 * - 2016.7 (by Lai Wei) Added this abstraction layer so that hpctraceviewer can display data from multiple sources.
 */
public abstract class AbstractColorTable {
	static final public int COLOR_ICON_SIZE = 8;
	final protected ColorImagePair imageWhite;
	final protected ColorImagePair imageGrey;
	
	// data members
	protected HashMap<Object, ColorImagePair> colorMatcher;
	
	/**All of the keys stored in this colorTable.*/
	protected ArrayList<Object> keys;
	
	/**The display this ColorTable uses to generate the random colors.*/
	protected Display display;
	
	public AbstractColorTable(IWorkbenchWindow window) {
		display = window.getShell().getDisplay();
		keys = new ArrayList<Object>();
		
		RGB rgb_white = new RGB(255, 255, 255); 
		imageWhite = new ColorImagePair( new Color(display, rgb_white));
		
		RGB rgb_grey = new RGB(128, 128, 128); 
		imageGrey = new ColorImagePair( new Color(display, rgb_grey));
	}
	
	public abstract void setColorTable();
	
	public void dispose() {
		for (ColorImagePair pair: colorMatcher.values()) {
			pair.dispose();
		}
		//imageWhite.dispose();
	}
	
	/**
	 * Returns the color that is used to separate different instances of the same thing.
	 */
	public Color getSeparatorColor()
	{
		return imageWhite.getColor();
	}
	
	/**
	 * Returns the color that corresponds to the key
	 * @param key
	 * @return
	 */
	public Color getColor(Object key)
	{
		if (colorMatcher.containsKey(key)) {
			return colorMatcher.get(key).getColor();
		} else {
			return null;
		}
	}
	
	/**
	 * returns the image that corresponds to the key
	 * @param key
	 * @return
	 */
	public Image getImage(Object key) 
	{
		if (colorMatcher.containsKey(key)) {
			return colorMatcher.get(key).getImage();
		} else {
			return null;
		}
	}
	
	/************************************************************************
	 * Adds a key to the list of keys in this ColorTable.
	 * NOTE: Doesn't create a color for this key. All the color creating
	 * is done in setColorTable.
	 * @param key The key to be added.
	 ************************************************************************/
	public void addKey(Object key)
	{
		if(!keys.contains(key))
			keys.add(key);
	}
	
	/***
	 * set the key with a new color
	 * @param nkey
	 * @param color
	 */
	public void setColor(Object key, RGB rgb) {
		// dispose old value
		final ColorImagePair oldValue = colorMatcher.get(key);
		if (oldValue != null) {
			oldValue.dispose();
		}
		// create new value
		final ColorImagePair newValue = new ColorImagePair(new Color(display,rgb));
		colorMatcher.put(key, newValue);
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
