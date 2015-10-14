/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jacques Bouthillier - Initial Implementation of the table view
 ******************************************************************************/
package org.eclipse.egerrit.dashboard.ui.internal.model;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egerrit.core.rest.ChangeInfo;
import org.eclipse.egerrit.dashboard.ui.internal.commands.table.AdjustMyStarredHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the review table view.
 *
 * @since 1.0
 */
public class UIReviewTable {
	private static Logger logger = LoggerFactory.getLogger(UIReviewTable.class);

	private final int TABLE_STYLE = (SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

	// ------------------------------------------------------------------------
	// Variables
	// ------------------------------------------------------------------------
	private TableViewer fViewer;

	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------

	public UIReviewTable() {

	}

	// ------------------------------------------------------------------------
	// Methods
	// ------------------------------------------------------------------------

	public Composite createTableViewerSection(Composite aParent) {
		Composite viewerForm = new Composite(aParent, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
		viewerForm.setLayout(new FillLayout());

		// Create the table viewer to maintain the list of reviews
		fViewer = new TableViewer(viewerForm, TABLE_STYLE);
		fViewer = buildAndLayoutTable(fViewer);

		// Set the content provider and the Label provider and the sorter
		fViewer.setContentProvider(new ReviewTableContentProvider());

		// Set the viewer for the provider
		ReviewTableLabelProvider tableProvider = new ReviewTableLabelProvider();
		fViewer.setLabelProvider(tableProvider);
		ReviewTableSorter.bind(fViewer);
		fViewer.setComparator(new ReviewTableSorter(7)); // sort by Updated, descending

		// Add a Key event and mouse down listener
		fViewer.getTable().addListener(SWT.MouseDown, mouseButtonListener);

		return viewerForm;
	}

	/**
	 * Create each column for the List of Reviews
	 *
	 * @param aParent
	 * @param aViewer
	 */
	private TableViewer buildAndLayoutTable(final TableViewer aViewer) {
		final Table table = aViewer.getTable();

		//Get the review table definition
		ReviewTableDefinition[] tableInfo = ReviewTableDefinition.values();
		int size = tableInfo.length;
		logger.debug("Table	Name	Width	Resize Moveable"); //$NON-NLS-1$
		for (int index = 0; index < size; index++) {
			logger.debug("index [ " + index + " ] " + tableInfo[index].getName() + "\t: " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					+ tableInfo[index].getWidth() + "\t: " + tableInfo[index].getResize() + "\t: " //$NON-NLS-1$ //$NON-NLS-2$
					+ tableInfo[index].getMoveable());
			createTableViewerColumn(tableInfo[index]);
		}

		TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);
		table.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				table.setRedraw(false);
				Point tableSize = table.getSize();
				Point parentSize = table.getParent().getSize();
				//Adjust the width  according to its parent
				int minimumTableWidth = ReviewTableDefinition.getMinimumWidth();
				int mimimumSubjectWidth = ReviewTableDefinition.SUBJECT.getWidth();
				int minProjectWidth = ReviewTableDefinition.PROJECT.getWidth();

				//Adjust the subject and project column to take the remaining space
				int scrollWidth = table.getVerticalBar().getSize().x;
				//If not visible, take the extra space
				if (!table.getVerticalBar().isVisible()) {
					scrollWidth = 0;
				}

				int computeExtraWidth = parentSize.x - 10 - (minimumTableWidth) - scrollWidth;
				int newSubjectWidth = mimimumSubjectWidth;
				int newProjectWidth = minProjectWidth;
				//If extra space, redistribute it to specific column
				if (computeExtraWidth > 0) {
					//Assign some to subject and some to Project
					int value = 2 * computeExtraWidth / 3;
					newSubjectWidth = mimimumSubjectWidth + value; // 2/3 of the extra
					newProjectWidth = minProjectWidth + computeExtraWidth - value; // 1/3 of the extra
				}
				//Subject column
				table.getColumn(2).setWidth(newSubjectWidth);
				//Project column
				table.getColumn(5).setWidth(newProjectWidth);

				table.setRedraw(true);
			}

			@Override
			public void controlMoved(ControlEvent e) {

			}
		});

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		return aViewer;
	}

	/**
	 * Create each column in the review table list
	 *
	 * @param ReviewTableDefinition
	 * @return TableViewerColumn
	 */
	private TableViewerColumn createTableViewerColumn(ReviewTableDefinition aTableInfo) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(fViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(aTableInfo.getName());
		column.setWidth(aTableInfo.getWidth());
		column.setAlignment(aTableInfo.getAlignment());
		column.setResizable(aTableInfo.getResize());
		column.setMoveable(aTableInfo.getMoveable());
		return viewerColumn;

	}

	private final Listener mouseButtonListener = new Listener() {
		public void handleEvent(Event aEvent) {
			logger.debug("mouseButtonListener() for " + aEvent.button); //$NON-NLS-1$
			switch (aEvent.type) {
			case SWT.MouseDown:
				// Left Click
				if (aEvent.button == 1) {

					// Process the Item table handling
					processItemSelection();

				}
				// For now, use button 2 to modify the starred value column 1
				if (aEvent.button == 2) {
					// Select the new item in the table
					Table table = fViewer.getTable();
					table.deselectAll();
					Point p = new Point(aEvent.x, aEvent.y);
					TableItem tbi = fViewer.getTable().getItem(p);
					if (tbi != null) {
						table.setSelection(tbi);
					}

					// Execute the command to adjust the column: ID with the
					// starred information
					AdjustMyStarredHandler handler = new AdjustMyStarredHandler();
					try {
						handler.execute(new ExecutionEvent());
					} catch (ExecutionException excutionException) {
//						StatusHandler.log(new Status(IStatus.ERROR, GerritCorePlugin.PLUGIN_ID,
//								excutionException.getMessage(), excutionException));
						logger.error(excutionException.getMessage());

					}
				}
				// Right Click
				if (aEvent.button == 3) {
					// Process the Item table handling
					// processItemSelection();
				}
				break;
			default:
				break;
			}
		}

	};

	/**
	 * Process the selected data from the item table
	 */
	private void processItemSelection() {
		ISelection tableSelection = fViewer.getSelection();
		if (!tableSelection.isEmpty()) {
			if (tableSelection instanceof IStructuredSelection) {
				Object obj = ((IStructuredSelection) tableSelection).getFirstElement();
				if (obj instanceof ChangeInfo) {
//					IAttributeContainer item = (IAttributeContainer) obj;
					logger.debug("Selected table OBJECT selection ID: " //$NON-NLS-1$
							+ ((ChangeInfo) obj).getId() + "\t subject: " //$NON-NLS-1$
							+ ((ChangeInfo) obj).getSubject());
//
//							+ item.getAttribute(GerritTask.SHORT_CHANGE_ID) + "\t subject: " //$NON-NLS-1$
//							+ item.getAttribute(GerritTask.SUBJECT));
				}
			}
		}
	}

	public TableViewer getViewer() {
		return fViewer;
	}
}
