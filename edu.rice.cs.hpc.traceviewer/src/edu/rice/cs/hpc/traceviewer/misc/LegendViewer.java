package edu.rice.cs.hpc.traceviewer.misc;

import java.util.ArrayList;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.data.util.string.StringUtil;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractDataController;
import edu.rice.cs.hpc.traceviewer.data.abstraction.AbstractProcessData;
import edu.rice.cs.hpc.traceviewer.data.abstraction.ProcessDataService;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.operation.BufferRefreshOperation;
import edu.rice.cs.hpc.traceviewer.operation.PositionOperation;
import edu.rice.cs.hpc.traceviewer.operation.TraceOperation;
import edu.rice.cs.hpc.traceviewer.services.DataService;

/**************************************************
 * A viewer for Legneds.
 *************************************************/
public class LegendViewer extends TableViewer
	implements IOperationHistoryListener{
	
	private final TableViewerColumn viewerColumn;
	
	private final ProcessDataService ptlService;
	
	private final DataService dataService;
	
	public LegendViewer(Composite parent, final HPCCallStackView csview)
	{
		super(parent, SWT.SINGLE | SWT.READ_ONLY );
		
		final IWorkbenchWindow window = (IWorkbenchWindow)csview.getSite().
				getWorkbenchWindow();
		final ISourceProviderService service = (ISourceProviderService) window.getService(ISourceProviderService.class);

		dataService = (DataService) service.getSourceProvider(DataService.DATA_PROVIDER);
				
		ptlService = (ProcessDataService) service.getSourceProvider(ProcessDataService.PROCESS_DATA_PROVIDER);
		
        final Table stack = this.getTable();
        
        GridData data = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        data.heightHint = 160;
        stack.setLayoutData(data);
        
        //------------------------------------------------
        // add content provider
        //------------------------------------------------
        this.setContentProvider( new IStructuredContentProvider(){

			public void dispose() {}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) { }

			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof ArrayList<?>) {
					Object o[] = ((ArrayList<?>) inputElement).toArray();
					return o;
				}
				return null;
			}
        	
        });
        
        stack.setVisible(false);
        
        stack.addListener(SWT.EraseItem, new Listener() {   
            @Override
            public void handleEvent(Event event) {
                event.detail &= ~SWT.SELECTED;
            }
        });
        
        //------------------------------------------------
        // add label provider
        //------------------------------------------------

		final ColumnLabelProvider myLableProvider = new ColumnLabelProvider() {
        	public Image getImage(Object element) {
        		if (element instanceof String) {
        			Image img = null;
        			AbstractDataController stData = dataService.getData();
        			if (stData != null)
        				img = stData.getColorTable().getImage((String)element);
        			return img;
        		}
        		
				return null;        		
        	}
        	
        	public String getText(Object element)
        	{
        		if (element instanceof String)
        			return (String) element;
        		return null;
        	}
        	
        	public String getToolTipText(Object element)
        	{
        		final String originalText = getText(element);
        		return StringUtil.wrapScopeName(originalText, 100);
        	}
        	
        	public int getToolTipDisplayDelayTime(Object object)
        	{
        		return 200;
        	}
		};
		viewerColumn = new TableViewerColumn(this, SWT.NONE);
		viewerColumn.setLabelProvider(myLableProvider);
		viewerColumn.getColumn().setWidth(100);
		getTable().setVisible(true);

		ColumnViewerToolTipSupport.enableFor(this, ToolTip.NO_RECREATE);
		
		TraceOperation.getOperationHistory().addOperationHistoryListener(this);
	}
	
	/***
	 * refresh the call stack in case there's a new data
	 */
	public void updateView()
	{
		AbstractDataController stData = dataService.getData();
		
		if (stData == null) {
			return;
		}
		// general case
		final ImageTraceAttributes attributes = stData.getAttributes();
    	int estimatedProcess = (attributes.getPosition().process - attributes.getProcessBegin());
    	int numDisplayedProcess = ptlService.getNumProcessData();
    	
    	// case for num displayed processes is less than the number of processes
    	estimatedProcess = (int) ((float)estimatedProcess* 
    			((float)numDisplayedProcess/(attributes.getProcessInterval())));
    	
    	// case for single process
    	estimatedProcess = Math.min(estimatedProcess, numDisplayedProcess-1);

    	AbstractProcessData ptl = ptlService.getProcessData(estimatedProcess);
    	if (ptl == null) return;
    	final ArrayList<String> names = ptl.getFrequentNames(stData.getAttributes().getDepth());
		
		final Display display = Display.getDefault();
		display.asyncExec( new Runnable() {
			@Override
			public void run() {
				setInput(names);
				viewerColumn.getColumn().pack();
			}
		} );
	}
	
	@Override
	public void historyNotification(final OperationHistoryEvent event) {
		final IUndoableOperation operation = event.getOperation();

		if (operation.hasContext(BufferRefreshOperation.context) ||
				operation.hasContext(PositionOperation.context)) {
			if (event.getEventType() == OperationHistoryEvent.DONE) {
				updateView();
			}
		}
	}
}
