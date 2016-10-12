/*******************************************************************************
 * Copyright (c) 2016 Ericsson AB and others.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson Communications - initial API and implementation
 *
 ******************************************************************************/
package org.eclipse.egerrit.internal.ui.editors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egerrit.internal.model.RevisionInfo;
import org.eclipse.egerrit.internal.ui.utils.ActiveWorkspaceRevision;
import org.eclipse.egerrit.internal.ui.utils.Messages;
import org.eclipse.egerrit.internal.ui.utils.UIUtils;

/**
 * This class implements the handler to open a dialog of the files associated to the active review
 *
 * @since 1.0
 */
public class OpenFileSelectionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ActiveWorkspaceRevision activeRevision = ActiveWorkspaceRevision.getInstance();
		RevisionInfo revInfo = activeRevision.getActiveRevision();
		if (revInfo != null) {
			FilesDialog filesDialog = new FilesDialog(revInfo, activeRevision.getGerritClient());
			filesDialog.open();
		} else {
			UIUtils.displayInformation(null, Messages.OpenFileProblem, Messages.OpenFileProblemMessage);
		}

		return null;
	}
}
