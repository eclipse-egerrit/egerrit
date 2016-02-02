/*******************************************************************************
 * Copyright (c) 2015 Ericsson AB.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jacques Bouthillier - initial API and implementation
 *******************************************************************************/

package org.eclipse.egerrit.ui.internal.tabs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;

import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.egerrit.core.EGerritCorePlugin;
import org.eclipse.egerrit.core.GerritClient;
import org.eclipse.egerrit.core.command.DeleteDraftRevisionCommand;
import org.eclipse.egerrit.core.command.DeleteReviewedCommand;
import org.eclipse.egerrit.core.command.GetReviewedFilesCommand;
import org.eclipse.egerrit.core.command.ListCommentsCommand;
import org.eclipse.egerrit.core.command.ListDraftsCommand;
import org.eclipse.egerrit.core.command.SetReviewedCommand;
import org.eclipse.egerrit.core.exception.EGerritException;
import org.eclipse.egerrit.internal.model.ChangeInfo;
import org.eclipse.egerrit.internal.model.CommentInfo;
import org.eclipse.egerrit.internal.model.FetchInfo;
import org.eclipse.egerrit.internal.model.FileInfo;
import org.eclipse.egerrit.internal.model.ModelPackage;
import org.eclipse.egerrit.internal.model.RevisionInfo;
import org.eclipse.egerrit.internal.model.impl.StringToFileInfoImpl;
import org.eclipse.egerrit.internal.model.impl.StringToRevisionInfoImpl;
import org.eclipse.egerrit.ui.EGerritUIPlugin;
import org.eclipse.egerrit.ui.editors.OpenCompareEditor;
import org.eclipse.egerrit.ui.internal.table.UIFilesTable;
import org.eclipse.egerrit.ui.internal.table.provider.ComboPatchSetLabelProvider;
import org.eclipse.egerrit.ui.internal.table.provider.FileTableLabelProvider;
import org.eclipse.egerrit.ui.internal.utils.LinkDashboard;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.databinding.EMFProperties;
import org.eclipse.emf.databinding.FeaturePath;
import org.eclipse.emf.databinding.IEMFListProperty;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This class is used in the editor to handle the Gerrit history view
 *
 * @since 1.0
 */
public class FilesTabView extends Observable implements PropertyChangeListener {

	// used to fire events of registered properties
	private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private static final String BASE = "Base"; //$NON-NLS-1$

	private static final String WORKSPACE = "Workspace"; //$NON-NLS-1$

	private static final String PATCHSET = "Patch Sets ";

	private static final String SEPARATOR = "/"; //$NON-NLS-1$

	private final String TITLE = "Gerrit Server ";

	private RevisionInfo fSelectedRevision = null;

	private GerritClient gerritClient;

	private ChangeInfo fChangeInfo;

	private TableViewer tableFilesViewer;

	private Label lblTotal;

	private Combo comboDiffAgainst;

	private Label lblPatchSet;

	private ComboViewer comboPatchsetViewer;

	private IDoubleClickListener fdoubleClickListener;

	private Button fDeleteDraftRevisionButton;

	// For the images
	private static ImageRegistry fImageRegistry = new ImageRegistry();

	private static final String ARROW_DOWN = "arrowDown.png"; //$NON-NLS-1$

	/**
	 * Note: An image registry owns all of the image objects registered with it, and automatically disposes of them the
	 * SWT Display is disposed.
	 */
	static {

		String iconPath = "icons/"; //$NON-NLS-1$

		fImageRegistry.put(ARROW_DOWN, EGerritUIPlugin.getImageDescriptor(iconPath + ARROW_DOWN));

	}

	// ------------------------------------------------------------------------
	// Constructor and life cycle
	// ------------------------------------------------------------------------

	/**
	 * @param tabFolder
	 * @param ChangeInfo
	 *            changeInfo
	 */
	public void create(GerritClient gerritClient, TabFolder tabFolder, ChangeInfo changeInfo) {
		this.gerritClient = gerritClient;
		fChangeInfo = changeInfo;
		createControls(tabFolder);
	}

	private void createControls(final TabFolder tabFolder) {
		final TabItem tbtmFiles = new TabItem(tabFolder, SWT.NONE);
		tbtmFiles.setText("Files");

		Composite composite = new Composite(tabFolder, SWT.NONE);
		tbtmFiles.setControl(composite);
		composite.setLayout(new GridLayout(10, false));

		lblPatchSet = new Label(composite, SWT.NONE);
		GridData gd_lblPatchSet = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		lblPatchSet.setLayoutData(gd_lblPatchSet);
		lblPatchSet.setText("Patch Sets (    /    )");

		comboPatchsetViewer = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);

		GridData gd_comboPatchSet = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		comboPatchsetViewer.getCombo().setLayoutData(gd_comboPatchSet);
		comboPatchsetViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof StructuredSelection) {
					IStructuredSelection structSel = (IStructuredSelection) selection;
					if (structSel.getFirstElement() instanceof StringToRevisionInfoImpl) {

						setPatchSetLabelCombo();
						setDiffAgainstCombo();
						StringToRevisionInfoImpl revInfo = (StringToRevisionInfoImpl) structSel.getFirstElement();

						setCurrentRevision(revInfo.getValue());
						//Adjust the reviewed files and the comments associated to the current patchset
						setFileTabFields();

						setChanged();
						notifyObservers();

					}
				}
			}
		});
		comboPatchsetViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				StringToRevisionInfoImpl rev1 = (StringToRevisionInfoImpl) e1;
				StringToRevisionInfoImpl rev2 = (StringToRevisionInfoImpl) e2;
				return rev2.getValue().get_number() - rev1.getValue().get_number();
			};
		});

		comboPatchsetViewer.getControl().addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				// ignore
			}

			@Override
			public void focusGained(FocusEvent e) {
				// To allow to display the whole list in the pull-down menu
				comboPatchsetViewer.getCombo().setItems(comboPatchsetViewer.getCombo().getItems());

			}
		});

		Label lblDiffAgainst = new Label(composite, SWT.NONE);
		GridData gd_DiffAgainst = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		lblDiffAgainst.setLayoutData(gd_DiffAgainst);
		lblDiffAgainst.setText("Diff against:");

		comboDiffAgainst = new Combo(composite, SWT.NONE);
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		comboDiffAgainst.setLayoutData(gd_combo);

		lblTotal = new Label(composite, SWT.NONE);
		GridData gd_lblTotal = new GridData(SWT.FILL, SWT.CENTER, true, false, 6, 1);
		lblTotal.setLayoutData(gd_lblTotal);
		lblTotal.setText("Total");

		fDeleteDraftRevisionButton = new Button(composite, SWT.BORDER);
		fDeleteDraftRevisionButton.setText("Delete Revision");
		GridData gd_deleteDraft = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
		fDeleteDraftRevisionButton.setLayoutData(gd_deleteDraft);
		fDeleteDraftRevisionButton.setToolTipText("Delete Draft Revision");
		fDeleteDraftRevisionButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog.openConfirm(fDeleteDraftRevisionButton.getParent().getShell(),
						"Delete draft revision", "Continue ?")) {
					return;
				}
				DeleteDraftRevisionCommand deleteDraftChangeCmd = gerritClient.deleteDraftRevision(fChangeInfo.getId(),
						fSelectedRevision.getId());
				try {
					deleteDraftChangeCmd.call();
					if (fChangeInfo.getRevisions().size() == 1) {
						IWorkbench workbench = PlatformUI.getWorkbench();
						final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();

						IEditorPart editor = activePage.getActiveEditor();
						activePage.closeEditor(editor, false);
					}
					LinkDashboard linkDash = new LinkDashboard(gerritClient);
					linkDash.invokeRefreshDashboardCommand("", ""); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (EGerritException e1) {
					EGerritCorePlugin.logError(gerritClient.getRepository().formatGerritVersion() + e1.getMessage());
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// ignore
			}
		});

		final Button fileViewPulldown = new Button(composite, SWT.BORDER);
		fileViewPulldown.setImage(fImageRegistry.get(ARROW_DOWN));
		GridData gd_FileView = new GridData(SWT.RIGHT, SWT.TOP, true, false, 1, 1);
		fileViewPulldown.setLayoutData(gd_FileView);
		fileViewPulldown.setToolTipText("File path layout");
		fileViewPulldown.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.widgets.Menu menu = new org.eclipse.swt.widgets.Menu(fileViewPulldown.getShell(),
						SWT.POP_UP);

				final MenuItem nameFirst = new MenuItem(menu, SWT.CHECK);
				nameFirst.setText("Show File Names first");
				final FileTableLabelProvider labelProvider = (FileTableLabelProvider) tableFilesViewer
						.getLabelProvider();
				nameFirst.setSelection(labelProvider.getFileOrder());

				nameFirst.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						MenuItem menuItem = (MenuItem) e.getSource();
						labelProvider.setFileNameFirst(menuItem.getSelection());
						tableFilesViewer.refresh();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						// ignore
					}
				});
				Point loc = fileViewPulldown.getLocation();
				Rectangle rect = fileViewPulldown.getBounds();

				Point mLoc = new Point(loc.x - 1, loc.y + rect.height);
				menu.setLocation(fileViewPulldown.getDisplay().map(fileViewPulldown.getParent(), null, mLoc));

				menu.setVisible(true);
			};

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// ignore
			}
		});

		UIFilesTable tableUIFiles = new UIFilesTable();
		tableUIFiles.createTableViewerSection(composite);

		tableFilesViewer = tableUIFiles.getViewer();
		tableFilesViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 10, 1));
		fdoubleClickListener = new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				Object element = sel.getFirstElement();
				if (element instanceof StringToFileInfoImpl) {
					FileInfo selectedFile = ((StringToFileInfoImpl) element).getValue();
					OpenCompareEditor compareEditor;
					if (!gerritClient.getRepository().getServerInfo().isAnonymous()) {
						showEditorTip();
					}
					compareEditor = new OpenCompareEditor(gerritClient, fChangeInfo);
					String diffSource = comboDiffAgainst.getText();
					if (diffSource.equals(WORKSPACE)) {
						compareEditor.compareAgainstWorkspace(selectedFile,
								getParticipant(selectedFile.getContainedIn().getId()));
					} else if (diffSource.equals(BASE)) {
						compareEditor.compareAgainstBase(fChangeInfo.getProject(), selectedFile,
								getParticipant(selectedFile.getContainedIn().getId()));
					} else {
						RevisionInfo rev = getRevisionInfoByNumber(diffSource);
						if (rev != null) {
							compareEditor.compareTwoRevisions(rev.getFiles().get(selectedFile.getOld_path()),
									selectedFile, getParticipant(selectedFile.getContainedIn().getId()));
							return;
						}
						MessageDialog.openError(null, "Can not open compare editor",
								"The compare editor can not yet show difference with " + diffSource);
					}
					setReviewedFlag(element);
				}
			}

		};

		tableFilesViewer.addDoubleClickListener(fdoubleClickListener);
		if (!gerritClient.getRepository().getServerInfo().isAnonymous()) {
			tableFilesViewer.getTable().addMouseListener(toggleReviewedStateListener());
		}

		//Set the binding for this section
		filesTabDataBindings(tableFilesViewer);
	}

	/**
	 * This method is for the command double click on a row
	 *
	 * @return none
	 */
	public void selectRow() {
		if (tableFilesViewer.getElementAt(0) != null) {
			tableFilesViewer.setSelection(new StructuredSelection(tableFilesViewer.getElementAt(0)), true);
			IStructuredSelection selection = (IStructuredSelection) tableFilesViewer.getSelection();
			final DoubleClickEvent event = new DoubleClickEvent(tableFilesViewer, selection);
			fdoubleClickListener.doubleClick(event);
		}
	}

	private void showEditorTip() {
		Preferences prefs = ConfigurationScope.INSTANCE.getNode("org.eclipse.egerrit.prefs");

		Preferences editorPrefs = prefs.node("editor");
		boolean choice = editorPrefs.getBoolean("editortip", false);

		if (choice) {
			return;
		}
		MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
				tableFilesViewer.getControl().getShell(), Messages.FileTabView_EGerriTip,
				Messages.FileTabView_EGerriTipValue, Messages.FileTabView_EGerriTipShowAgain, false, null, null);

		if (dialog.getToggleState()) {
			editorPrefs.putBoolean("editortip", true);
			try {
				editorPrefs.flush();
			} catch (BackingStoreException e) {
				//There is not much we can do
			}
		}
		return;
	}

	private Runnable getParticipant(final String revisionId) {
		return new Runnable() {

			@Override
			public void run() {
				update();
			}
		};
	}

	private void computeTotals() {
		int lineInserted = 0;
		int lineDeleted = 0;
		int numDrafts = 0;
		int numComment = 0;

		RevisionInfo revInfo = fSelectedRevision;
		EMap<String, FileInfo> mapFiles = revInfo.getFiles();
		Collection<FileInfo> filesInfo = mapFiles.values();

		if (!filesInfo.isEmpty()) {
			Iterator<FileInfo> itr1 = filesInfo.iterator();
			while (itr1.hasNext()) {
				FileInfo fileInfo = itr1.next();
				lineInserted += fileInfo.getLines_inserted();
				lineDeleted += fileInfo.getLines_deleted();
				if (fileInfo.getDraftComments() != null) {
					numDrafts += fileInfo.getDraftComments().size();
				}
				if (fileInfo.getNewComments() != null) {
					numComment += fileInfo.getNewComments().size();
				}
			}
			StringBuilder sb = new StringBuilder();
			sb.append("Summary:\tTotal changes: ");
			sb.append("+"); //$NON-NLS-1$
			sb.append(Integer.toString(lineInserted));
			sb.append("/"); //$NON-NLS-1$
			sb.append("-"); //$NON-NLS-1$
			sb.append(Integer.toString(lineDeleted));
			if (numDrafts != 0) {
				sb.append("\t\t");
				sb.append("Drafts: ");
				sb.append(numDrafts);
			}
			if (numComment != 0) {
				sb.append("\t\t");
				sb.append("Comments: ");
				sb.append(numComment);
			}
			lblTotal.setText(sb.toString());
		}
	}

	private void setReviewedFlag(Object element) {
		if (!gerritClient.getRepository().getServerInfo().isAnonymous()) {
			FileInfo fileInfo = ((StringToFileInfoImpl) element).getValue();
			if (!fileInfo.isReviewed()) {
				SetReviewedCommand command = gerritClient.setReviewed(fChangeInfo.getId(),
						fSelectedRevision.getCommit().getCommit(), fileInfo.getOld_path());
				try {
					command.call();
				} catch (EGerritException ex) {
					EGerritCorePlugin.logError(gerritClient.getRepository().formatGerritVersion() + ex.getMessage());
				}

				fileInfo.setReviewed(true);

				tableFilesViewer.refresh();
			}
		}
	}

	/**
	 * This method is the listener to toggle a file's reviewed state
	 *
	 * @return MouseAdapter
	 */
	private MouseAdapter toggleReviewedStateListener() {
		return new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				ViewerCell viewerCell = tableFilesViewer.getCell(new Point(e.x, e.y));
				if (viewerCell != null && viewerCell.getColumnIndex() == 0) {
					//Selected the first column, so we can send the delete option
					//Otherwise, do not delete
					ISelection selection = tableFilesViewer.getSelection();
					if (selection instanceof IStructuredSelection) {

						IStructuredSelection structuredSelection = (IStructuredSelection) selection;

						Object element = structuredSelection.getFirstElement();

						FileInfo fileInfo = ((StringToFileInfoImpl) element).getValue();
						toggleReviewed(fileInfo);

					}
				}
			}

		};
	}

	private void toggleReviewed(FileInfo fileInfo) {
		if (fileInfo.isReviewed()) {
			DeleteReviewedCommand command = gerritClient.deleteReviewed(fChangeInfo.getId(),
					fSelectedRevision.getCommit().getCommit(), fileInfo.getOld_path());
			try {
				command.call();
				fileInfo.setReviewed(false);
			} catch (EGerritException ex) {
				EGerritCorePlugin.logError(gerritClient.getRepository().formatGerritVersion() + ex.getMessage());
			}
		} else {
			SetReviewedCommand command = gerritClient.setReviewed(fChangeInfo.getId(),
					fSelectedRevision.getCommit().getCommit(), fileInfo.getOld_path());
			try {
				command.call();
			} catch (EGerritException ex) {
				EGerritCorePlugin.logError(gerritClient.getRepository().formatGerritVersion() + ex.getMessage());
			}
			fileInfo.setReviewed(true);
		}
		tableFilesViewer.refresh();
	}

	protected void filesTabDataBindings(TableViewer tableFilesViewer) {

		//Set the PatchSet Combo viewer
		if (comboPatchsetViewer != null) {
			ObservableListContentProvider contentPatchSetProvider = new ObservableListContentProvider();
			comboPatchsetViewer.setContentProvider(contentPatchSetProvider);
			IEMFListProperty revisionsProperty = EMFProperties.list(ModelPackage.Literals.CHANGE_INFO__REVISIONS);
			IObservableMap[] observePatchSetMaps = Properties.observeEach(contentPatchSetProvider.getKnownElements(),
					EMFProperties.value(ModelPackage.Literals.REVISION_INFO__NUMBER));
			comboPatchsetViewer.setLabelProvider(new ComboPatchSetLabelProvider(observePatchSetMaps));
			comboPatchsetViewer.setInput(revisionsProperty.observe(fChangeInfo));

		}

		//Set the FilesViewer
		if (tableFilesViewer != null) {
			FeaturePath changerev = FeaturePath.fromList(ModelPackage.Literals.CHANGE_INFO__REVISION,
					ModelPackage.Literals.REVISION_INFO__FILES);

			IObservableList revisionsChanges = EMFProperties.list(changerev).observe(fChangeInfo);

			final FeaturePath reviewed = FeaturePath.fromList(ModelPackage.Literals.STRING_TO_FILE_INFO__VALUE,
					ModelPackage.Literals.FILE_INFO__REVIEWED);
			final FeaturePath status = FeaturePath.fromList(ModelPackage.Literals.STRING_TO_FILE_INFO__VALUE,
					ModelPackage.Literals.FILE_INFO__STATUS);
			final FeaturePath filePath = FeaturePath.fromList(ModelPackage.Literals.STRING_TO_FILE_INFO__VALUE,
					ModelPackage.Literals.FILE_INFO__OLD_PATH);
			final FeaturePath comment = FeaturePath.fromList(ModelPackage.Literals.STRING_TO_FILE_INFO__VALUE,
					ModelPackage.Literals.FILE_INFO__NEW_COMMENTS);
			final FeaturePath size = FeaturePath.fromList(ModelPackage.Literals.STRING_TO_FILE_INFO__VALUE,
					ModelPackage.Literals.FILE_INFO__LINES_INSERTED);

			ObservableListContentProvider contentProvider = new ObservableListContentProvider();
			tableFilesViewer.setContentProvider(contentProvider);
			final IObservableMap[] watchedProperties = Properties
					.observeEach(contentProvider.getKnownElements(),
							new IValueProperty[] { EMFProperties.value(reviewed), EMFProperties.value(status),
									EMFProperties.value(filePath), EMFProperties.value(comment),
									EMFProperties.value(size) });
			tableFilesViewer.setLabelProvider(new FileTableLabelProvider(watchedProperties));
			tableFilesViewer.setInput(revisionsChanges);
		}
	}

	/**
	 * Fill the combo box with all patch set to compare with except the selected one. Also, add the Base and the
	 * Workspace which is the default one
	 */
	private void setDiffAgainstCombo() {
		List<String> listPatch = new ArrayList<String>();
		//Always start the list with the following items
		listPatch.add(BASE);
		listPatch.add(WORKSPACE);
		//Find the current Patchset selection
		ISelection selected = comboPatchsetViewer.getSelection();

		if (!selected.isEmpty()) {
			int psSelected = -1;
			if (selected instanceof StructuredSelection) {
				Object element = ((IStructuredSelection) selected).getFirstElement();
				if (element instanceof StringToRevisionInfoImpl) {
					RevisionInfo selectInfo = ((StringToRevisionInfoImpl) element).getValue();
					psSelected = selectInfo.get_number();
				}
			}
			//Display the list of patchset without the current one
			List<RevisionInfo> listRevision = new ArrayList<RevisionInfo>(fChangeInfo.getRevisions().values());
			Collections.sort(listRevision, new Comparator<RevisionInfo>() {

				@Override
				public int compare(RevisionInfo rev1, RevisionInfo rev2) {
					return rev2.get_number() - rev1.get_number();
				}
			});

			Iterator<RevisionInfo> it = listRevision.iterator();
			while (it.hasNext()) {
				RevisionInfo rev = it.next();
				if (rev.get_number() != psSelected) {
					listPatch.add(Integer.toString(rev.get_number()));
				}
			}

			comboDiffAgainst.setItems(listPatch.toArray(new String[0]));
			comboDiffAgainst.select(0);//Select the Base to compare with by default

		}
	}

	/**
	 * - Fill the label showing which patch set is selected from the total list of patchset
	 */
	private void setPatchSetLabelCombo() {
		//Find the current PatchSet selection
		ISelection selected = comboPatchsetViewer.getSelection();
		if (!selected.isEmpty()) {
			int psSelected = -1;
			if (selected instanceof StructuredSelection) {
				Object element = ((IStructuredSelection) selected).getFirstElement();
				if (element instanceof StringToRevisionInfoImpl) {
					RevisionInfo selectInfo = ((StringToRevisionInfoImpl) element).getValue();
					psSelected = selectInfo.get_number();
				}
			}
			StringBuilder sb = new StringBuilder();
			sb.append(PATCHSET);
			sb.append(psSelected);
			sb.append(SEPARATOR);
			sb.append(fChangeInfo.getRevisions().size());

			//Display the selected patchSet
			lblPatchSet.setText(sb.toString());

		}
	}

	private void fillReviewedRevisionFileInfo() {
		RevisionInfo revInfo = fSelectedRevision;

		// enable reviewed file status
		if (!gerritClient.getRepository().getServerInfo().isAnonymous()) {
			String[] reviewedFiles = queryVerified(gerritClient, fChangeInfo.getId(), revInfo.getId(),
					new NullProgressMonitor());
			if (reviewedFiles != null) {//List can be null if there was a conflict  from the query
				List<String> reviewedFilesList = Arrays.asList(reviewedFiles);
				EMap<String, FileInfo> mapFiles = revInfo.getFiles();
				Iterator<FileInfo> file = mapFiles.values().iterator();
				while (file.hasNext()) {
					FileInfo fi = file.next();
					for (String e : reviewedFilesList) {
						if (e.compareTo(fi.getOld_path()) == 0) {
							fi.setReviewed(true);
						}
					}
				}
			}
		}
	}

	private void fillCommentsRevisionFileInfo() {
		RevisionInfo revInfo = fSelectedRevision;

		Map<String, ArrayList<CommentInfo>> mapCommentInfo = queryComments(gerritClient, fChangeInfo.getId(),
				revInfo.getId(), new NullProgressMonitor());

		//Fill the Files table with comments
		EMap<String, FileInfo> mapFiles = revInfo.getFiles();
		Iterator<FileInfo> file = mapFiles.values().iterator();
		while (file.hasNext()) {
			FileInfo fi = file.next();
			Iterator<Map.Entry<String, ArrayList<CommentInfo>>> commentIter = mapCommentInfo.entrySet().iterator();
			while (commentIter.hasNext()) {
				Entry<String, ArrayList<CommentInfo>> entryComment = commentIter.next();
				//Add comments associated to the file
				if (fi.getOld_path().equals(entryComment.getKey())) {
					fi.getNewComments().clear();
					fi.getNewComments().addAll(entryComment.getValue());
					break;
				}
			}
		}

		//Query for the DRAFTS comments
		Map<String, ArrayList<CommentInfo>> mapDraftCommentInfo = queryDraftComments(gerritClient, fChangeInfo.getId(),
				revInfo.getId(), new NullProgressMonitor());

		//Fill the Files table
		Iterator<FileInfo> fileIt = mapFiles.values().iterator();
		while (fileIt.hasNext() && mapDraftCommentInfo != null) {
			FileInfo displayFileInfo = fileIt.next();
			Iterator<Map.Entry<String, ArrayList<CommentInfo>>> commentIter = mapDraftCommentInfo.entrySet().iterator();
			while (commentIter.hasNext()) {
				Entry<String, ArrayList<CommentInfo>> entryComment = commentIter.next();
				//Add comments associated to the file
				if (displayFileInfo.getOld_path().equals(entryComment.getKey())) {
					displayFileInfo.getDraftComments().clear();
					displayFileInfo.getDraftComments().addAll(entryComment.getValue());
					break;
				}
			}
		}

	}

	/**
	 * Fill the data fields included in the Files Tab
	 */
	private void setFileTabFields() {
		if (tableFilesViewer != null && !tableFilesViewer.getTable().isDisposed()) {
			fillReviewedRevisionFileInfo();
			fillCommentsRevisionFileInfo();
			tableFilesViewer.refresh();
			computeTotals();
		}
	}

	/***************************************************************/
	/*                                                             */
	/* Section to QUERY the data structure                         */
	/*                                                             */
	/************************************************************* */
	private Map<String, ArrayList<CommentInfo>> queryComments(GerritClient gerrit, String change_id, String revision_id,
			IProgressMonitor monitor) {
		try {
			monitor.beginTask("Executing query", IProgressMonitor.UNKNOWN);

			ListCommentsCommand command = gerrit.getListComments(change_id, revision_id);

			Map<String, ArrayList<CommentInfo>> res = null;
			try {
				res = command.call();
				return res;
			} catch (EGerritException e) {
				EGerritCorePlugin.logError(gerrit.getRepository().formatGerritVersion() + e.getMessage());
			}
		} finally {
			monitor.done();
		}

		return null;

	}

	/***************************************************************/
	/*                                                             */
	/* Section to QUERY the files that were reviewed               */
	/*                                                             */
	/************************************************************* */
	private String[] queryVerified(GerritClient gerrit, String change_id, String revision_id,
			IProgressMonitor monitor) {
		try {
			monitor.beginTask("Executing query", IProgressMonitor.UNKNOWN);

			GetReviewedFilesCommand command = gerrit.getReviewed(change_id, revision_id);

			String[] res = null;
			try {
				res = command.call();
				return res;
			} catch (EGerritException e) {
				EGerritCorePlugin.logError(gerrit.getRepository().formatGerritVersion() + e.getMessage());
			}
		} finally {
			monitor.done();
		}

		return null;

	}

	private Map<String, ArrayList<CommentInfo>> queryDraftComments(GerritClient gerrit, String change_id,
			String revision_id, IProgressMonitor monitor) {

		try {
			if (gerrit.getRepository().getServerInfo().isAnonymous()) {
				return null;
			}
			monitor.beginTask("Executing query", IProgressMonitor.UNKNOWN);

			ListDraftsCommand command = gerrit.listDraftsComments(change_id, revision_id);
			Map<String, ArrayList<CommentInfo>> res = null;
			try {
				res = command.call();
				return res;
			} catch (EGerritException e) {
				EGerritCorePlugin.logError(gerrit.getRepository().formatGerritVersion() + e.getMessage());
			}
		} finally {
			monitor.done();
		}

		return null;
	}

	/**
	 * @return the tableFilesViewer
	 */
	public TableViewer getTableFilesViewer() {
		return tableFilesViewer;
	}

	private void setCurrentRevision(RevisionInfo revisionInfo) {
		fChangeInfo.setCurrent_revision(revisionInfo.getId());
		fSelectedRevision = revisionInfo;
		fDeleteDraftRevisionButton.setEnabled(
				fChangeInfo.getStatus().compareTo("DRAFT") == 0 && fSelectedRevision.isActionAllowed("publish")); //$NON-NLS-1$
	}

	/**
	 * Notify the registered observer
	 */
	@Override
	public void notifyObservers() {
		super.notifyObservers();
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// ignore

	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}

	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * @return the patchset selection as a string
	 */
	public String getSelectedPatchSet() {
		String psSelected = null;
		ISelection selected = comboPatchsetViewer.getSelection();
		if (!selected.isEmpty()) {
			psSelected = findSelectedPatchsetRef(selected, psSelected);
		}
		return psSelected;
	}

	/**
	 * @return the patchset selection ID as a string
	 */
	public String getSelectedPatchSetID() {
		RevisionInfo psSelectedRev = null;
		ISelection selected = comboPatchsetViewer.getSelection();
		if (!selected.isEmpty()) {
			psSelectedRev = findSelectedPatchsetRevision(selected);
			return psSelectedRev.getId();
		}
		return null;
	}

	/**
	 * @return the patchset selection number as an int
	 */
	public int getSelectedPatchSetNumber() {
		RevisionInfo psSelectedRev = null;
		ISelection selected = comboPatchsetViewer.getSelection();
		if (!selected.isEmpty()) {
			psSelectedRev = findSelectedPatchsetRevision(selected);
			return psSelectedRev.get_number();
		}
		return -1;
	}

	/**
	 * Initialize the combo patch set viewer with the latest patch set
	 */
	public void setInitPatchSet() {
		ISelection selection = comboPatchsetViewer.getSelection();
		if (selection.isEmpty()) {
			//Select the latest patch-set and trigger the selection change
			//The latest patch set is the first one in the list since we sort the data
			//in this order
			comboPatchsetViewer.getCombo().select(0);
			comboPatchsetViewer.setSelection(comboPatchsetViewer.getSelection(), true);
		}
	}

	/**
	 * @param selected
	 * @param psSelected
	 * @return String The patch set reference when a patch set is selected
	 */
	private String findSelectedPatchsetRef(ISelection selected, String psSelected) {
		RevisionInfo selectInfo = findSelectedPatchsetRevision(selected);
		EMap<String, FetchInfo> fetchInfoMap = selectInfo.getFetch();
		FetchInfo fetchInfo = fetchInfoMap.get("git"); //$NON-NLS-1$
		if (fetchInfo != null) {
			psSelected = fetchInfo.getRef().toString();
		} else {
			fetchInfo = fetchInfoMap.get("ssh"); //$NON-NLS-1$
			if (fetchInfo != null) {
				psSelected = fetchInfo.getRef().toString();
			} else {
				fetchInfo = fetchInfoMap.get("http"); //$NON-NLS-1$
				if (fetchInfo != null) {
					psSelected = fetchInfo.getRef().toString();
				}
			}
		}
		return psSelected;
	}

	/**
	 * @param selected
	 * @return RevisionInfo The patch set reference when a patch set is selected
	 */
	private RevisionInfo findSelectedPatchsetRevision(ISelection selected) {
		RevisionInfo selectInfo = null;
		if (selected instanceof StructuredSelection) {
			Object element = ((IStructuredSelection) selected).getFirstElement();
			if (element instanceof StringToRevisionInfoImpl) {
				selectInfo = ((StringToRevisionInfoImpl) element).getValue();
			}
		}
		return selectInfo;
	}

	private RevisionInfo getRevisionInfoByNumber(String number) {
		return fChangeInfo.getRevisionByNumber(Integer.valueOf(number));
	}

	public void update() {
		RevisionInfo revInfo = fSelectedRevision;

		setCurrentRevision(revInfo);
		setFileTabFields();
		setChanged();
		notifyObservers();
	}

	/**
	 * allows to receive an updated version of changeinfo which in turn is used to determine enablement of buttons
	 *
	 * @param changeInfo
	 * @return none
	 */
	public void setChangeInfo(ChangeInfo changeInfo) {
		fChangeInfo = changeInfo;
	}
}
