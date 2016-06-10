package edu.rice.cs.hpc.viewer.action;

import javax.annotation.PostConstruct;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class MessageLabel {
	static final private int MESSAGE_TIMEOUT = 8000; // time out when showing a message

	protected Color clrGREEN, clrYELLOW, clrRED;
	
	private Label lblMessage;
	
	public MessageLabel(Composite parent) {
	}

	@PostConstruct
	public void postConstruct(Composite parent) {
		lblMessage = new Label(parent, SWT.LEFT);
    	GridDataFactory.fillDefaults().grab(true, false).applyTo(lblMessage);
		
		final Display display = Display.getCurrent();
		
		this.clrYELLOW = display.getSystemColor(SWT.COLOR_YELLOW);
		this.clrRED = display.getSystemColor(SWT.COLOR_RED);
		this.clrGREEN = display.getSystemColor(SWT.COLOR_GREEN);
	}
	
	
	/**
	 * Show a message with information style (with green background)
	 */
	public void showInfoMessage(String sMsg) {
		lblMessage.setBackground(this.clrGREEN);
		lblMessage.setText(sMsg);
		// remove the msg in 5 secs
		RestoreMessageThread thrRestoreMessage = new RestoreMessageThread();
		thrRestoreMessage.start();
	}
	
	/**
	 * Show a warning message (with yellow background).
	 * The caller has to remove the message and restore it to the original state
	 * by calling restoreMessage() method
	 */
	public void showWarningMessagge(String sMsg) {
		lblMessage.setBackground(this.clrYELLOW);
		lblMessage.setText(sMsg);
		// remove the msg in 5 secs
		RestoreMessageThread thrRestoreMessage = new RestoreMessageThread();
		thrRestoreMessage.start();
	}
	
	/**
	 * Show an error message on the message bar. It is the caller responsibility to 
	 * remove the message
	 * @param sMsg
	 */
	public void showErrorMessage(String sMsg) {
		lblMessage.setBackground(this.clrRED);
		lblMessage.setText(" " + sMsg);
		// remove the msg in 5 secs
		RestoreMessageThread thrRestoreMessage = new RestoreMessageThread();
		thrRestoreMessage.start();
	}

	/**
	 * Restore the message bar into the original state
	 */
	private void restoreMessage() {
		lblMessage.setBackground(lblMessage.getBackground());
		lblMessage.setText("");
	}
	
	/**
	 * Class to restoring the background of the message bar by waiting for 5 seconds
	 * TODO: we need to parameterize the timing for the wait
	 * @author la5
	 *
	 */
	private class RestoreMessageThread extends Thread {	
		RestoreMessageThread() {
			super();
		}
         public void run() {
             try{
            	 sleep(MESSAGE_TIMEOUT);
             } catch(InterruptedException e) {
            	 e.printStackTrace();
             }
             // need to run from UI-thread for restoring the background
             // without UI-thread we will get SWTException !!
        	 Display display = Display.getCurrent();
        	 if (display != null && !display.isDisposed()) {
        		 display.asyncExec(new Runnable() {
                	 public void run() {
                    	 restoreMessage();
                	 }
                 });
        	 }
         }
     }


}
