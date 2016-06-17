package edu.rice.cs.hpc.viewer.framework;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

// for a extended example see
// https://bugs.eclipse.org/382224
public class LifeCycleManager 
{
	@PostContextCreate
	void postContextCreate(final IEventBroker eventBroker, IApplicationContext context, Display display) {
		// register for startup completed event and close the shell 
		eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE,
				new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				System.out.println("i am here");

				eventBroker.unsubscribe(this);
			}
		});
		// close static splash screen
		context.applicationRunning();
	}	  
}
