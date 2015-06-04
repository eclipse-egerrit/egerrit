/*******************************************************************************
 * Copyright (c) 2014 Ericsson AB and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jacques Bouthillier - initial API and implementation
 *******************************************************************************/

package org.eclipse.egerrit.dashboard.ui.internal.commands;

import org.eclipse.osgi.util.NLS;

/**
 * This class implements the internalization of the strings.
 *
 * @since 1.0
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.egerrit.dashboard.ui.internal.commands.messages"; //$NON-NLS-1$

	public static String AddGerritSiteHandler_commandAdd;

	public static String AddGerritSiteHandler_uiJOB;

	public static String SelectReviewSiteHandler_exception;

	public static String SelectReviewSiteHandler_searchCommand;

	public static String SelectReviewSiteHandler_dashboardUiJob;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
