/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jacques Bouthillier - Initial Implementation of the plug-in utility
 ******************************************************************************/

package org.eclipse.egerrit.dashboard.ui.internal.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egerrit.dashboard.ui.GerritUi;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * This class implements the Gerrit Dashboard UI utility.
 * 
 * @since 1.0
 */

public class UIUtils {

	/**
	 * Method notInplementedDialog.
	 *
	 * @param String
	 */
	public static void notInplementedDialog(String aSt) {
		GerritUi.Ftracer.traceWarning(Messages.UIUtils_notImplemented);
		final ErrorDialog dialog = new ErrorDialog(null, Messages.UIUtils_dashboardInformation,
				NLS.bind(Messages.UIUtils_methodNotReady, aSt),
				new Status(IStatus.INFO, GerritUi.PLUGIN_ID, 0, Messages.UIUtils_notImplemented, null), IStatus.INFO);
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		// TODO later we will want to do this automatically
	}

	/**
	 * Method showErrorDialog.
	 *
	 * @param String
	 *            message
	 * @param String
	 *            reason
	 */
	public static void showErrorDialog(String aMsg, String aReason) {
		GerritUi.Ftracer.traceWarning(aMsg + "\t reason: " + aReason);
		final ErrorDialog dialog = new ErrorDialog(null, Messages.UIUtils_dashboardInfo, aMsg,
				new Status(IStatus.INFO, GerritUi.PLUGIN_ID, 0, aReason, null), IStatus.INFO);
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
	}

	/**
	 * Creates view preference frame and return the child composite.
	 *
	 * @param aParent
	 *            the parent composite.
	 * @return the child composite.
	 */
	public static Composite createsGeneralComposite(Composite aParent, int style) {
		Composite child = new Composite(aParent, style);
		FormLayout layout = new FormLayout();
		layout.marginWidth = 10;
		layout.marginHeight = 0;
		// child.setSize(WIDTH, HEIGHT);
		child.setLayout(layout);
		// child.minHeight = 100;
		return child;
	}

}
