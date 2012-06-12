package edu.rice.cs.hpc.traceviewer.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import edu.rice.cs.hpc.traceviewer.spaceTimeData.ColorTable;

/****
 * 
 * display procedure and its class
 * can be used for either adding or editing the map
 *
 */
public class ProcedureMapDetailDialog extends Dialog {

	final private String title;
	private String proc;
	private String description;
	private RGB rgb;
	
	private Text txtProc;
	private Text txtClass;

	/***
	 * retrieve the new procedure name
	 * @return
	 */
	public String getProcedure() {
		return proc;
	}
	
	/**
	 * retrieve the new class
	 * @return
	 */
	public String getDescription() {
		return description;
	}
	
	public RGB getRGB() {
		return rgb;
	}
	
	/***
	 * constructor
	 * 
	 * @param parentShell : parent shell
	 * @param title : title of the dialog
	 * @param proc : default name of the procedure
	 * @param procClass : default name of the class
	 */
	protected ProcedureMapDetailDialog(Shell parentShell, String title, String proc, String procClass, RGB color) {
		super(parentShell);
		
		this.proc = proc;
		this.description = procClass;
		this.title = title;
		this.rgb = color;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
		
		final Label lblProc = new Label(composite, SWT.LEFT);
		lblProc.setText("Procedure: ");
		txtProc = new Text(composite, SWT.LEFT | SWT.SINGLE);
		txtProc.setText(proc);
		GridDataFactory.swtDefaults().hint(
				this.convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT)
				.grab(true, false).applyTo(txtProc);
		
		final Label lblClass = new Label(composite, SWT.LEFT);
		lblClass.setText("Description: ");
		txtClass = new Text(composite, SWT.LEFT | SWT.SINGLE);
		txtClass.setText(description);
		GridDataFactory.swtDefaults().hint(
				this.convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT)
				.grab(true, false).applyTo(txtClass);
		
		final Label lblColor = new Label(composite, SWT.LEFT);
		lblColor.setText("Color: ");
		final Button btnColor = new Button(composite, SWT.PUSH | SWT.FLAT);
		btnColor.computeSize(ColorTable.COLOR_ICON_SIZE, ColorTable.COLOR_ICON_SIZE);
		if (rgb == null) {
			rgb = getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK).getRGB();
		}
		setButtonImage(btnColor, rgb);
		
		btnColor.addSelectionListener( new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				final Shell shell = ProcedureMapDetailDialog.this.getShell();
				ColorDialog colorDlg = new ColorDialog(shell);
				colorDlg.setRGB(rgb);
				colorDlg.setText("Select color for " + ProcedureMapDetailDialog.this.description);
				final RGB newRGB = colorDlg.open();
				if (newRGB != null) {
					rgb = newRGB;
					setButtonImage(btnColor, rgb);
				}
			}
		});
		
		return composite;
	}

	/***
	 * set an image to a button given the color 
	 * @param button
	 * @param color
	 */
	private void setButtonImage(Button button, RGB color) {
		Image oldImage = button.getImage();
		if (oldImage != null) {
			oldImage.dispose();
		}
		Image image = ColorTable.createImage(getShell().getDisplay(), color);
		button.setImage(image);
	}
	
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (title != null) {
			shell.setText(title);
		}
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		proc = txtProc.getText();
		description = txtClass.getText();
		super.okPressed();
	}
	
	/***
	 * unit test
	 * 
	 * @param argv
	 */
	static public void main(String argv[]) {
		Display display = new Display ();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		shell.open();
		
		ProcedureMapDetailDialog dlg = new ProcedureMapDetailDialog(shell, "edit", "procedure", "procedure-class", null);

		dlg.open();
		
		System.out.println("proc: " + dlg.proc + ", class: " + dlg.description);
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		
		display.dispose();
	}

}
