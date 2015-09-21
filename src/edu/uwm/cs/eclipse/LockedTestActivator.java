package edu.uwm.cs.eclipse;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class LockedTestActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "edu.uwm.cs.locked-tests";

	// The shared instance
	private static LockedTestActivator plugin;

	/**
	 * The constructor
	 */
	public LockedTestActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		System.out.println("UWM CS Locked Test plugin activated.");
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static LockedTestActivator getDefault() {
		if (plugin == null) {
			plugin = new LockedTestActivator();
		}
		return plugin;
	}

	/**
	 * Return image from plugin.
	 * @param path
	 * @return
	 * @author VonC @ stackoverflow
	 */
	public Image getImage(String path) {
		Image image = getDefault().getImageRegistry().get(path);
		if (image == null) {
			getImageRegistry().put(path, AbstractUIPlugin.
					imageDescriptorFromPlugin(PLUGIN_ID, path));
			image = getImageRegistry().get(path);
		}
		return image;
	}

	public ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

}
