package edu.rice.cs.hpc.traceviewer.depth;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.traceviewer.services.DataService;
import edu.rice.cs.hpc.traceviewer.ui.AbstractTimeView;

/*****************************************************
 * 
 * Depth view
 *
 *****************************************************/
public class HPCDepthView extends AbstractTimeView
{
	public static final String ID = "hpcdepthview.view";
	
	/**The composite that holds everything in the view*/
	Composite master;
	
	/**The max depth of the space time data.*/
	int maxDepth;
	
	/** Paints and displays the detail view. */
	DepthTimeCanvas depthCanvas;
		
	public void createPartControl(Composite _master)
	{
		master = _master;
		
		setupEverything();
		setListener();
		super.addListener();
	}
	
	private void setupEverything()
	{
		/*************************************************************************
		 * Master Composite
		 */
		
		master.setLayout(new GridLayout());
		master.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/*************************************************************************
		 * Depth View Canvas
		 */
		
		depthCanvas = new DepthTimeCanvas(master);
		depthCanvas.setLayout(new GridLayout());
		depthCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		depthCanvas.setVisible(false);		
	}
	
	private void setListener() {
		ISourceProviderService service = (ISourceProviderService)getSite().getService(ISourceProviderService.class);
		final ISourceProvider serviceProvider = service.getSourceProvider(DataService.DATA_UPDATE);
		serviceProvider.addSourceProviderListener( new ISourceProviderListener(){

			public void sourceChanged(int sourcePriority, Map sourceValuesByName) {	}
			public void sourceChanged(int sourcePriority, String sourceName,
					Object sourceValue) {
				// eclipse bug: even if we set a very specific source provider, eclipse still
				//	gather event from other source. we then require to put a guard to avoid this.
				if (sourceName.equals(DataService.DATA_UPDATE)) {
					depthCanvas.refresh();
				}
			}
		});		
	}

	public void updateView()
	{
		ISourceProviderService service = (ISourceProviderService) this.getSite().
				getWorkbenchWindow().getService(ISourceProviderService.class);
		DataService dataService = (DataService) service.getSourceProvider(DataService.DATA_PROVIDER);
		this.depthCanvas.updateView(dataService);
		depthCanvas.setVisible(true);
	}

	public void setFocus()
	{
		this.depthCanvas.setFocus();
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.traceviewer.ui.IActiveNotification#active(boolean)
	 */
	public void active(boolean isActive) 
	{
		depthCanvas.activate(isActive);
	}
}
