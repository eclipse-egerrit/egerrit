/*******************************************************************************
 * Copyright (c) 2015-2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jacques Bouthillier - Initial Implementation of the files table
 ******************************************************************************/

package org.eclipse.egerrit.internal.ui.table.model;

import java.util.ArrayList;

import org.eclipse.egerrit.internal.ui.utils.Messages;
import org.eclipse.swt.SWT;

/**
 * This class implements the files table.
 *
 * @since 1.0
 */
// ------------------------------------------------------------------------
// Constants
// ------------------------------------------------------------------------
// Definition of the Conflict with table {name, width of the column, Resizeable,
// Moveable, Alignment}
public enum FilesTableModel implements ITableModel {
	// 			Name 			                               Width 	Resize Moveable Alignment
	REVIEWED("", 30, true, true, SWT.LEFT), //$NON-NLS-1$
	MOD_TYPE("", 30, true, true, SWT.LEFT), //$NON-NLS-1$
	FILE_PATH(Messages.FilesTableDefinition_filePath, 540, true, true, SWT.LEFT), //
	COMMENTS(Messages.FilesTableDefinition_comments, 220, true, true, SWT.LEFT), //
	SIZE(Messages.FilesTableDefinition_size, 80, false, true, SWT.LEFT);

	private final String fHeader;

	private int fwidth;

	private final int fInitialwidth;

	private final boolean fResize;

	private final boolean fMoveable;

	private final int fAlignment;

	private boolean fVisible = true;

	private FilesTableModel(String aName, int aWidth, boolean aResize, boolean aMove, int align) {
		fHeader = aName;
		fwidth = aWidth;
		fInitialwidth = aWidth;
		fResize = aResize;
		fMoveable = aMove;
		fAlignment = align;
		fVisible = true;
	}

	public String getName() {
		return fHeader;
	}

	public int getWidth() {
		return fwidth;
	}

	public boolean getResize() {
		return fResize;
	}

	public boolean getMoveable() {
		return fMoveable;
	}

	public int getAlignment() {
		return fAlignment;
	}

	public String[] getColumnName() {
		ArrayList<String> listName = new ArrayList<String>();
		for (ITableModel st : FilesTableModel.values()) {
			listName.add(st.getName());
		}
		return listName.toArray(new String[] {});
	}

	public int getMinimumWidth() {
		int width = 0;
		for (int index = 0; index < FilesTableModel.values().length; index++) {
			width += FilesTableModel.values()[index].getWidth();
		}
		return width;
	}

	public boolean isColumnVisible() {
		return fVisible;
	}

	public void setColumnVisible(boolean value) {
		fVisible = value;
		if (value) {
			fwidth = fInitialwidth;
		} else {
			fwidth = 0;
		}
	}

	/**
	 * Provide an index array width the default order definition
	 *
	 * @return int[]
	 */
	public static int[] getDefaultOrder() {
		int size = FilesTableModel.values().length;
		int[] order = new int[size];
		for (int index = 0; index < size; index++) {
			order[index] = index;
		}
		return order;
	}
}
