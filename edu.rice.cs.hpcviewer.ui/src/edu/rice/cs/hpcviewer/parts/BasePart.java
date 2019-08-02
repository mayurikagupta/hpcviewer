package edu.rice.cs.hpcviewer.parts;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import edu.rice.cs.hpcviewer.components.ViewerCoolBar;
import edu.rice.cs.hpcviewer.constants.PartConstants;

public abstract class BasePart 
implements PartConstants
{
	private TableViewer tableViewer;
	
	@PostConstruct
	public void createControls(Composite parent, EMenuService menuService) {
		
		ViewerCoolBar coolBar = getCoolBar(parent);
		
		Composite tableComposite = new Composite(parent, SWT.NONE);
		
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);
		tableViewer =
		    new TableViewer(tableComposite,
		            SWT.MULTI |
		            SWT.H_SCROLL |
		            SWT.V_SCROLL |
		            SWT.FULL_SELECTION |
		            SWT.BORDER);
		
		TableViewerColumn col = new TableViewerColumn(tableViewer, SWT.NONE);
		col.getColumn().setText("Scope");
		
		tableViewer.getTable().setHeaderVisible(true);
		ColumnWeightData colData = new ColumnWeightData(TABLE_WEIGHT, TABLE_MIN_WIDTH, true);
		tableColumnLayout.setColumnData(col.getColumn(), colData);
	}

	abstract ViewerCoolBar getCoolBar(Composite parent);
}
