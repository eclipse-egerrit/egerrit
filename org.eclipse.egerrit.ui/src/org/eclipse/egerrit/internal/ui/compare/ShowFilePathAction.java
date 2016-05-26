/*******************************************************************************
 * Copyright (c) 2016 Ericsson AB.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson - initial API and implementation
 *******************************************************************************/

package org.eclipse.egerrit.internal.ui.compare;

import java.util.function.Supplier;

import org.eclipse.egerrit.internal.ui.EGerritImages;
import org.eclipse.egerrit.internal.ui.table.model.FilesTableModel;
import org.eclipse.egerrit.internal.ui.table.provider.FileInfoCompareCellLabelProvider;
import org.eclipse.egerrit.internal.ui.utils.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;

public class ShowFilePathAction extends Action {

	private Supplier<TreeViewer> viewer;

	public ShowFilePathAction(Supplier<TreeViewer> viewerRef) {
		this.viewer = viewerRef;
		setDescription(Messages.ShowFilePathAction_0);
		setToolTipText(Messages.ShowFilePathAction_1);
		setImageDescriptor(EGerritImages.getDescriptor(EGerritImages.TOGGLE_FILEPATH));
	}

	@Override
	public void run() {
		TreeViewer treeViewer = viewer.get();
		//Get the label provider for the FilePath column
		IBaseLabelProvider labelProvider = treeViewer.getLabelProvider(FilesTableModel.FILE_PATH.ordinal());
		if (labelProvider instanceof FileInfoCompareCellLabelProvider) {
			FileInfoCompareCellLabelProvider infoProvider = (FileInfoCompareCellLabelProvider) labelProvider;
			infoProvider.setFileNameFirst(!infoProvider.getFileOrder());
			treeViewer.refresh();
		}
	}
}
