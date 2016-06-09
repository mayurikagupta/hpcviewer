package edu.rice.cs.hpc.viewer.resources;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ResourceProvider 
{
	private ImageRegistry reg;
	
	public Image getImage(String key)
	{
		final ImageRegistry reg = getLocalImageRegistry();
		return reg.get(key);
	}
	
	private ImageRegistry getLocalImageRegistry()
	{
		if (reg != null)
			return reg;
		
		// Get the bundle using the universal method to get it from the current class
		Bundle b = FrameworkUtil.getBundle(getClass());  
		
		// Create a local image registry
		reg = new ImageRegistry();

		// Then fill the values...
		reg.put(ResourceConstant.IMG_ACTION_CHECK_COLUMNS, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_ACTION_CHECK_COLUMNS)));
		reg.put(ResourceConstant.IMG_ACTION_DERIVED_METRIC, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_ACTION_DERIVED_METRIC)));
		reg.put(ResourceConstant.IMG_ACTION_FONT_BIGGER, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_ACTION_FONT_BIGGER)));
		reg.put(ResourceConstant.IMG_ACTION_FONT_SMALLER, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_ACTION_FONT_SMALLER)));
		reg.put(ResourceConstant.IMG_CCT_THREAD, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_CCT_THREAD)));
		reg.put(ResourceConstant.IMG_CCT_THREAD_MAP, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_CCT_THREAD_MAP)));
		reg.put(ResourceConstant.IMG_NODE_CALLFROM, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_NODE_CALLFROM)));
		reg.put(ResourceConstant.IMG_NODE_CALLFROM_DISABLED, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_NODE_CALLFROM_DISABLED)));
		reg.put(ResourceConstant.IMG_NODE_CALLTO, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_NODE_CALLTO)));
		reg.put(ResourceConstant.IMG_NODE_CALLTO_DISABLED, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_NODE_CALLTO_DISABLED)));
		reg.put(ResourceConstant.IMG_TREE_FLATTEN, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_TREE_FLATTEN)));
		reg.put(ResourceConstant.IMG_TREE_HOTPATH, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_TREE_HOTPATH)));
		reg.put(ResourceConstant.IMG_TREE_SAVECSV, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_TREE_SAVECSV)));
		reg.put(ResourceConstant.IMG_TREE_UNFLATTEN, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_TREE_UNFLATTEN)));
		reg.put(ResourceConstant.IMG_TREE_ZOOMIN, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_TREE_ZOOMIN)));
		reg.put(ResourceConstant.IMG_TREE_ZOOMOUT, 
				ImageDescriptor.createFromURL(b.getEntry(ResourceConstant.IMG_TREE_ZOOMOUT)));

		return reg;
	}
}
