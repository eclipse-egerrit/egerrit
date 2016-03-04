/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jacques Bouthillier - Initial Implementation of the plug-in utility
 ******************************************************************************/

package org.eclipse.egerrit.ui.internal.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egerrit.ui.EGerritUIPlugin;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * This class implements the Gerrit UI utility.
 *
 * @since 1.0
 */

public class UIUtils {

	private static final Pattern COMMENT_PATTERN = Pattern.compile("(\\d+ comme[nt])(\\w+)"); //$NON-NLS-1$

	/**
	 * Method notInplementedDialog.
	 *
	 * @param String
	 */
	public static void notInplementedDialog(String aSt) {
		final ErrorDialog dialog = new ErrorDialog(null, Messages.UIUtils_Info,
				NLS.bind(Messages.UIUtils_methodNotReady, aSt),
				new Status(IStatus.INFO, EGerritUIPlugin.PLUGIN_ID, 0, Messages.UIUtils_notImplemented, null),
				IStatus.INFO);
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
		final ErrorDialog dialog = new ErrorDialog(null, Messages.UIUtils_Info, aMsg,
				new Status(IStatus.INFO, EGerritUIPlugin.PLUGIN_ID, 0, aReason, null), IStatus.INFO);
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
	}

	/**
	 * To display some information to the end-user
	 *
	 * @param shell
	 * @param title
	 * @param message
	 */
	public static void displayInformation(final Shell shell, final String title, final String message) {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(shell, title, message);
			}
		});
	}

	/**
	 * Compute the size of the Fonts used for this composite
	 *
	 * @param composite
	 * @return Point
	 */
	public static Point computeFontSize(Composite composite) {
		GC gc = new GC(composite);
		FontMetrics fm = gc.getFontMetrics();
		int charWidth = fm.getAverageCharWidth();

		gc.dispose();
		return new Point(charWidth, fm.getHeight());
	}

	/**
	 * Remove the link argument and return the text
	 *
	 * @return String
	 */
	public static String getLinkText(String text) {
		text = text.replaceFirst("<a>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replaceFirst("</a>", ""); //$NON-NLS-1$//$NON-NLS-2$
		return text.trim();
	}

	/**
	 * Test to see if the string has some kind of indication for comments
	 *
	 * @param msg
	 * @return boolean
	 */
	public static boolean hasComments(String msg) {
		Matcher matcher = COMMENT_PATTERN.matcher(msg.toLowerCase());
		return matcher.find(0);
	}
}
