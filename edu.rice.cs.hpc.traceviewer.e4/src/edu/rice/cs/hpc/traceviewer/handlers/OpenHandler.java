/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <lars.Vogel@gmail.com> - Bug 419770
 *******************************************************************************/
package edu.rice.cs.hpc.traceviewer.handlers;

import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.traceviewer.db.TraceDatabase;


public class OpenHandler 
{

	@Execute
	public void execute(Shell shell, MApplication application, 
			EModelService modelService, ECommandService commandService, EMenuService menuService) {
		
		
		if (TraceDatabase.openLocalDatabase(null, null)) {
		}		
	}
}
