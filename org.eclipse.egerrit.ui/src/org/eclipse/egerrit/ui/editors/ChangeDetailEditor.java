/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Guy Perron - initial API and implementation
 *     Jacques Bouthillier - Fill the message tab
 *     Jacques Bouthillier - Refactor the class
 *
 *******************************************************************************/

package org.eclipse.egerrit.ui.editors;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.conversion.NumberToStringConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.egerrit.core.EGerritCorePlugin;
import org.eclipse.egerrit.core.GerritClient;
import org.eclipse.egerrit.core.command.AbandonCommand;
import org.eclipse.egerrit.core.command.ChangeOption;
import org.eclipse.egerrit.core.command.CherryPickRevisionCommand;
import org.eclipse.egerrit.core.command.DeleteDraftChangeCommand;
import org.eclipse.egerrit.core.command.GetChangeCommand;
import org.eclipse.egerrit.core.command.GetRevisionActionsCommand;
import org.eclipse.egerrit.core.command.ListBranchesCommand;
import org.eclipse.egerrit.core.command.PublishDraftChangeCommand;
import org.eclipse.egerrit.core.command.RebaseCommand;
import org.eclipse.egerrit.core.command.RestoreCommand;
import org.eclipse.egerrit.core.command.RevertCommand;
import org.eclipse.egerrit.core.command.SetReviewCommand;
import org.eclipse.egerrit.core.command.SubmitCommand;
import org.eclipse.egerrit.core.exception.EGerritException;
import org.eclipse.egerrit.core.rest.AbandonInput;
import org.eclipse.egerrit.core.rest.CherryPickInput;
import org.eclipse.egerrit.core.rest.RebaseInput;
import org.eclipse.egerrit.core.rest.RestoreInput;
import org.eclipse.egerrit.core.rest.RevertInput;
import org.eclipse.egerrit.core.rest.ReviewInput;
import org.eclipse.egerrit.core.rest.SubmitInput;
import org.eclipse.egerrit.internal.model.ActionInfo;
import org.eclipse.egerrit.internal.model.BranchInfo;
import org.eclipse.egerrit.internal.model.ChangeInfo;
import org.eclipse.egerrit.internal.model.LabelInfo;
import org.eclipse.egerrit.internal.model.ModelPackage;
import org.eclipse.egerrit.internal.model.RevisionInfo;
import org.eclipse.egerrit.ui.EGerritUIPlugin;
import org.eclipse.egerrit.ui.editors.model.ChangeDetailEditorInput;
import org.eclipse.egerrit.ui.internal.tabs.FilesTabView;
import org.eclipse.egerrit.ui.internal.tabs.HistoryTabView;
import org.eclipse.egerrit.ui.internal.tabs.MessageTabView;
import org.eclipse.egerrit.ui.internal.tabs.SummaryTabView;
import org.eclipse.egerrit.ui.internal.utils.GerritToGitMapping;
import org.eclipse.egerrit.ui.internal.utils.LinkDashboard;
import org.eclipse.egit.ui.internal.dialogs.CheckoutConflictDialog;
import org.eclipse.egit.ui.internal.fetch.FetchGerritChangeWizard;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.databinding.EMFProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.services.IServiceLocator;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.ibm.icu.text.NumberFormat;

public class ChangeDetailEditor<ObservableObject> extends EditorPart implements PropertyChangeListener, Observer {
	private static final String REBASE = "rebase";

	private static final String SUBMIT = "submit";

	private static final String RESTORE = "restore";

	private static final String ABANDON = "abandon";

	private static final String VERIFIED = "Verified";

	private static final String CODE_REVIEW = "Code-Review";

	private static final String REVERT = "revert";

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String EDITOR_ID = "org.eclipse.egerrit.ui.editors.ChangeDetailEditor"; //$NON-NLS-1$

	private final static String TITLE = "Gerrit Server ";

	public SummaryTabView summaryTab = null;

	private HistoryTabView historytab = null;

	private MessageTabView messageTab = null;

	public FilesTabView filesTab = null;

	private ChangeInfo fChangeInfo;

	private GerritClient fGerritClient = null;

	private Text subjectData;

	private Text statusData;

	private Text shortIdData;

	private Button fSubmitRevert;

	private Button fAbandonRestore;

	private Button fRebase;

	private Button fCherryPick;

	private Button fReply;

	private String anonymousUserToolTip;

	private boolean fAbandonMode = false;

	private boolean fSubmitMode = true;

	private Button fDraftPublishDelete;

	private Composite headerSection;

	// ------------------------------------------------------------------------
	// Constructor and life cycle
	// ------------------------------------------------------------------------

	/**
	 * The constructor.
	 */
	public ChangeDetailEditor() {
		super();
	}

	@Override
	public void createPartControl(final Composite parent) {
		anonymousUserToolTip = "This button is disabled because you are connected anonymously to "
				+ fGerritClient.getRepository().getServerInfo().getServerURI() + ".";

		parent.setLayout(new GridLayout(1, false));
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		headerSection = headerSection(parent);
		headerSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tabFolder.setBackground(parent.getBackground());

		summaryTab = new SummaryTabView();
		summaryTab.create(fGerritClient, tabFolder, fChangeInfo);

		messageTab = new MessageTabView();
		messageTab.create(fGerritClient, tabFolder, fChangeInfo);
		messageTab.addObserver(this);

		filesTab = new FilesTabView();
		filesTab.create(fGerritClient, tabFolder, fChangeInfo);
		filesTab.addObserver(this);

		historytab = new HistoryTabView();
		historytab.create(fGerritClient, tabFolder, fChangeInfo);

		Composite compButton = buttonSection(parent);
		compButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		setPartName(((ChangeDetailEditorInput) getEditorInput()).getName());

		//This query fill the current revision
		setCurrentRevisionAndMessageTab(fGerritClient, fChangeInfo.getId());

		//Initialize the files tab with the latest patch-set
		filesTab.setInitPatchSet();

		//Queries to fill the Summary Review tab data
		if (summaryTab != null) {
			summaryTab.setTabs(fGerritClient, fChangeInfo);
		}

		buttonsEnablement();
	}

	private Composite headerSection(final Composite parent) {
		Group group_header = new Group(parent, SWT.NONE);
		group_header.setLayout(new GridLayout(5, false));
		group_header.setBackground(parent.getBackground());

		Label lblId = new Label(group_header, SWT.NONE);
		lblId.setText("ID:");

		shortIdData = new Text(group_header, SWT.NONE);
		shortIdData.setEditable(false);
		shortIdData.setBackground(parent.getBackground());
		shortIdData.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

		statusData = new Text(group_header, SWT.LEFT);
		GridData gd_lblStatus = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		statusData.setLayoutData(gd_lblStatus);
		statusData.setEditable(false);
		statusData.addListener(SWT.Modify, new Listener() {

			@Override
			public void handleEvent(Event event) {
				statusData.getParent().layout();
			}
		});

		Label lblSubject = new Label(group_header, SWT.NONE);
		lblSubject.setText("Subject:");
		GridData gd_Subject = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_Subject.horizontalIndent = 10;
		lblSubject.setLayoutData(gd_Subject);

		subjectData = new Text(group_header, SWT.NONE);
		GridData gd_lblSubjectData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		subjectData.setLayoutData(gd_lblSubjectData);
		subjectData.setEditable(false);
		subjectData.setBackground(parent.getBackground());
		subjectData.addListener(SWT.Modify, new Listener() {

			@Override
			public void handleEvent(Event event) {
				subjectData.getParent().layout();
			}
		});

		//Set the binding for this section
		headerSectionDataBindings();
		return group_header;
	}

	private Composite buttonSection(final Composite parent) {
		final int NUMBER_OF_BUTTONS = 8;
		final Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(NUMBER_OF_BUTTONS, true));

		Button refresh = new Button(c, SWT.PUSH);
		refresh.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		refresh.setText("Refresh");
		refresh.setToolTipText("Reload this change from the server.");
		refresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				refreshStatus();
			}
		});

		fSubmitRevert = new Button(c, SWT.PUSH);
		fSubmitRevert.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fSubmitRevert.setText("Submit");
		fSubmitRevert.setEnabled(false);
		if (fGerritClient.getRepository().getServerInfo().isAnonymous()) {
			fSubmitRevert.setToolTipText(anonymousUserToolTip);
			fSubmitRevert.addListener(SWT.MouseHover, new Listener() {

				@Override
				public void handleEvent(Event event) {
					// ignore

				}
			});
		} else {
			fSubmitRevert.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);

					if (fSubmitMode == true) {
						SubmitCommand submitCmd = fGerritClient.submit(fChangeInfo.getId());
						SubmitInput submitInput = new SubmitInput();
						submitInput.setWait_for_merge(true);

						submitCmd.setCommandInput(submitInput);

						try {
							submitCmd.call();
						} catch (EGerritException e3) {
							EGerritCorePlugin
									.logError(fGerritClient.getRepository().formatGerritVersion() + e3.getMessage());
						}
						fSubmitMode = false;
						refreshStatus();
						//Initialize the files tab with the latest patch-set
						filesTab.setInitPatchSet();
					} else {
						String revertMsg = "Revert \"" + fChangeInfo.getSubject() + "\"\n\n" + "This reverts commit "
								+ fChangeInfo.getCurrent_revision() + "\nReview number: " + fChangeInfo.get_number()
								+ ".";
						RevertCommand revertCmd = fGerritClient.revert(fChangeInfo.getId());
						RevertInput revertInput = new RevertInput();
						revertInput.setMessage(revertMsg);

						revertCmd.setCommandInput(revertInput);

						ChangeInfo revertResult = null;
						try {
							revertResult = revertCmd.call();
						} catch (EGerritException e3) {
							EGerritCorePlugin
									.logError(fGerritClient.getRepository().formatGerritVersion() + e3.getMessage());
						}
						openAnotherEditor(revertResult);
					}
				}
			});
		}

		fAbandonRestore = new Button(c, SWT.PUSH);
		fAbandonRestore.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fAbandonRestore.setEnabled(false);
		if (fGerritClient.getRepository().getServerInfo().isAnonymous()) {
			fAbandonRestore.setToolTipText(anonymousUserToolTip);
		} else {
			fAbandonRestore.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);

					if (fAbandonMode == true) {
						InputDialog inputDialog = new InputDialog(fAbandonRestore.getParent().getShell(),
								"Abandon message", "Enter the abandon message", "", null);
						if (inputDialog.open() != Window.OK) {
							return;
						}

						AbandonCommand abandonCmd = fGerritClient.abandon(fChangeInfo.getId());
						AbandonInput abandonInput = new AbandonInput();
						abandonInput.setMessage(inputDialog.getValue());

						abandonCmd.setCommandInput(abandonInput);

						try {
							abandonCmd.call();
						} catch (EGerritException e3) {
							EGerritCorePlugin
									.logError(fGerritClient.getRepository().formatGerritVersion() + e3.getMessage());
						}
						fAbandonMode = false;
						refreshStatus();
					} else {
						InputDialog inputDialog = new InputDialog(fAbandonRestore.getParent().getShell(),
								"Restore message", "Enter the restore message", "", null);
						if (inputDialog.open() != Window.OK) {
							return;
						}

						RestoreCommand restoreCmd = fGerritClient.restore(fChangeInfo.getId());
						RestoreInput restoreInput = new RestoreInput();
						restoreInput.setMessage(inputDialog.getValue());

						restoreCmd.setCommandInput(restoreInput);

						try {
							restoreCmd.call();
						} catch (EGerritException e3) {
							EGerritCorePlugin
									.logError(fGerritClient.getRepository().formatGerritVersion() + e3.getMessage());
						}
						fAbandonMode = true;
						refreshStatus();

					}
				}
			});
		}

		fRebase = new Button(c, SWT.PUSH);
		fRebase.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fRebase.setText("Rebase");
		if (fGerritClient.getRepository().getServerInfo().isAnonymous()) {
			fRebase.setToolTipText(anonymousUserToolTip);
			fRebase.setEnabled(false);
		} else {
			fRebase.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);

					InputDialog inputDialog = new InputDialog(fRebase.getParent().getShell(),
							"Code Review - Rebase Change",
							"Change parent revision (leave empty to rebase on targeted branch)", "", null);
					if (inputDialog.open() != Window.OK) {
						return;
					}

					RebaseCommand rebaseCmd = fGerritClient.rebase(fChangeInfo.getId());
					RebaseInput rebaseInput = new RebaseInput();
					rebaseInput.setBase(inputDialog.getValue());

					rebaseCmd.setCommandInput(rebaseInput);

					try {
						rebaseCmd.call();
					} catch (EGerritException e1) {
						if (e1.getCode() == EGerritException.SHOWABLE_MESSAGE) {
							MessageDialog.open(MessageDialog.INFORMATION, null, "Rebase failed",
									"Gerrit could not perform the rebase automatically. You need to perform the rebase locally.",
									SWT.NONE);
						} else {
							EGerritCorePlugin
									.logError(fGerritClient.getRepository().formatGerritVersion() + e1.getMessage());
						}
					}
					refreshStatus();
				}
			});
		}

		Button checkout = new Button(c, SWT.PUSH);
		checkout.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		checkout.setText("Checkout");
		checkout.addSelectionListener(checkoutButtonListener(parent));

		fCherryPick = new Button(c, SWT.PUSH);
		fCherryPick.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fCherryPick.setText("Cherry-Pick");
		if (fGerritClient.getRepository().getServerInfo().isAnonymous()) {
			fCherryPick.setToolTipText(anonymousUserToolTip);
			fCherryPick.setEnabled(false);
		} else {
			fCherryPick.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);

					BranchInfo[] listBranchesCmdResult = listBranches();

					List<String> listBranchesRef = new ArrayList<String>();
					Iterator<BranchInfo> it = Arrays.asList(listBranchesCmdResult).iterator();
					while (it.hasNext()) {
						listBranchesRef.add(it.next().getRef());
					}

					final CherryPickDialog cherryPickDialog = new CherryPickDialog(fCherryPick.getParent().getShell(),
							listBranchesRef, fChangeInfo.getRevision().getCommit().getMessage());
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							int ret = cherryPickDialog.open();
							if (ret == IDialogConstants.OK_ID) {
								cherryPickRevision(fChangeInfo.getId(), fChangeInfo.getCurrent_revision(),
										cherryPickDialog.getBranch(), cherryPickDialog.getMessage());
							}
						}

					});
				}
			});
		}

		fReply = new Button(c, SWT.PUSH | SWT.DROP_DOWN | SWT.ARROW_DOWN);
		fReply.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fReply.setText("Reply...");
		if (fGerritClient.getRepository().getServerInfo().isAnonymous()) {
			fReply.setToolTipText(anonymousUserToolTip);
			fReply.setEnabled(false);
		} else {
			fReply.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					org.eclipse.swt.widgets.Menu menu = new org.eclipse.swt.widgets.Menu(shell, SWT.POP_UP);

					final MenuItem itemReply = new MenuItem(menu, SWT.PUSH);
					itemReply.setText("Reply...");
					itemReply.addSelectionListener(new SelectionListener() {

						@Override
						public void widgetSelected(SelectionEvent e) {
							// ignore
							final ReplyDialog replyDialog = new ReplyDialog(itemReply.getParent().getShell(),
									fChangeInfo.getPermitted_labels(), fChangeInfo.getLabels());
							Display.getDefault().syncExec(new Runnable() {
								public void run() {
									int ret = replyDialog.open();
									if (ret == IDialogConstants.OK_ID) {
										//Fill the data structure for the reply
										ReviewInput reviewInput = new ReviewInput();
										reviewInput.setMessage(replyDialog.getMessage());
										reviewInput.setLabels(replyDialog.getRadiosSelection());
										reviewInput.setDrafts(ReviewInput.DRAFT_PUBLISH);
										// JB which field e-mail	reviewInput.setNotify(replyDialog.getEmail());
										//Send the data
										postReply(reviewInput);
									}

								}
							});
						}

						@Override
						public void widgetDefaultSelected(SelectionEvent e) {
							// ignore
						}
					});

					MenuItem itemCRPlus2 = new MenuItem(menu, SWT.PUSH);
					itemCRPlus2.setText("Code-Review+2");
					String latestPatchSet = fChangeInfo.getLatestPatchSet().getId();

					//Test if we should allow the +2 button or not
					//Condition:
					//     - User is a committer (maxCRDefined == maxCRPermitted)
					//     - If user permitted, the maxCRPermitted > 0 if not Abandoned
					//     - Selected patch set is the latest
					//     - User cannot submit yet
					int maxCRPermitted = findMaxPermitted(CODE_REVIEW);
					int maxCRDefined = findMaxDefinedLabelValue(CODE_REVIEW);
					boolean allowPlus2 = false;
					if (maxCRDefined == maxCRPermitted && maxCRPermitted > 0) {
						allowPlus2 = true;
					}
					//Verify the patchset if we are allowed only, no need to check if not allowed
					if (allowPlus2) {
						String psSelectedID = filesTab.getSelectedPatchSetID();
						if (!(psSelectedID != null && latestPatchSet.compareTo(psSelectedID) == 0)) {
							allowPlus2 = false;
						}
					}

					itemCRPlus2.setEnabled(!canSubmit() && allowPlus2);

					if (itemCRPlus2.isEnabled()) {
						itemCRPlus2.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								super.widgetSelected(e);
								// Code-Review +2
								ReviewInput reviewInput = new ReviewInput();
								reviewInput.setDrafts(ReviewInput.DRAFT_PUBLISH);
								Map<String, String> obj = new HashMap<String, String>();
								obj.put(CODE_REVIEW, "2");

								reviewInput.setLabels(obj);
								postReply(reviewInput);
							}
						});
					}

					Point loc = fReply.getLocation();
					Rectangle rect = fReply.getBounds();

					Point mLoc = new Point(loc.x - 1, loc.y + rect.height);

					menu.setLocation(shell.getDisplay().map(fReply.getParent(), null, mLoc));

					menu.setVisible(true);
				}
			});
		}

		fDraftPublishDelete = new Button(c, SWT.PUSH | SWT.DROP_DOWN | SWT.ARROW_DOWN);
		fDraftPublishDelete.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fDraftPublishDelete.setText("Draft...");
		if (fGerritClient.getRepository().getServerInfo().isAnonymous()) {
			fDraftPublishDelete.setToolTipText(anonymousUserToolTip);
			fDraftPublishDelete.setEnabled(false);
		} else {
			fDraftPublishDelete.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					super.widgetSelected(e);
					org.eclipse.swt.widgets.Menu menu = new org.eclipse.swt.widgets.Menu(fDraftPublishDelete.getShell(),
							SWT.POP_UP);

					final MenuItem itemPublish = new MenuItem(menu, SWT.PUSH);
					itemPublish.setText("Publish");
					itemPublish.addSelectionListener(new SelectionListener() {

						@Override
						public void widgetSelected(SelectionEvent e) {
							PublishDraftChangeCommand publishCommand = fGerritClient
									.publishDraftChange(fChangeInfo.getId());
							try {
								publishCommand.call();
								refreshStatus();
							} catch (EGerritException e1) {
								EGerritCorePlugin.logError(
										fGerritClient.getRepository().formatGerritVersion() + e1.getMessage());
							}

						}

						@Override
						public void widgetDefaultSelected(SelectionEvent e) {
							// ignore
						}
					});

					MenuItem itemDeleteDraftChange = new MenuItem(menu, SWT.PUSH);
					itemDeleteDraftChange.setText("Delete");

					final IEditorSite site = getEditorSite();
					itemDeleteDraftChange.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							super.widgetSelected(e);
							if (!MessageDialog.openConfirm(fDraftPublishDelete.getParent().getShell(),
									"Delete draft review", "Continue ?")) {
								return;
							}
							DeleteDraftChangeCommand deleteDraftChangeCmd = fGerritClient
									.deleteDraftChange(fChangeInfo.getId());
							try {
								deleteDraftChangeCmd.call();
								IWorkbench workbench = PlatformUI.getWorkbench();
								final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();

								IEditorPart editor = activePage.getActiveEditor();
								activePage.closeEditor(editor, false);
								LinkDashboard linkDash = new LinkDashboard(fGerritClient);
								linkDash.invokeRefreshDashboardCommand("", ""); //$NON-NLS-1$ //$NON-NLS-2$
							} catch (EGerritException e1) {
								EGerritCorePlugin.logError(
										fGerritClient.getRepository().formatGerritVersion() + e1.getMessage());
							}
						}
					});

					Point loc = fDraftPublishDelete.getLocation();
					Rectangle rect = fDraftPublishDelete.getBounds();

					Point mLoc = new Point(loc.x - 1, loc.y + rect.height);

					menu.setLocation(fDraftPublishDelete.getDisplay().map(fDraftPublishDelete.getParent(), null, mLoc));

					menu.setVisible(true);
				}
			});

		}

		c.setSize(c.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return c;
	}

	private ChangeInfo cherryPickRevision(String changeId, String revisionId, String branch, String message) {
		CherryPickRevisionCommand cherryPickCmd = fGerritClient.cherryPickRevision(changeId, revisionId);
		CherryPickInput cherryPickInput = new CherryPickInput();
		cherryPickInput.setDestination(branch);
		cherryPickInput.setMessage(message);

		cherryPickCmd.setCommandInput(cherryPickInput);
		ChangeInfo listBranchesCmdResult = null;
		try {
			listBranchesCmdResult = cherryPickCmd.call();
		} catch (EGerritException e3) {
			EGerritCorePlugin.logError(fGerritClient.getRepository().formatGerritVersion() + e3.getMessage());
		}
		return listBranchesCmdResult;
	}

	private BranchInfo[] listBranches() {
		ListBranchesCommand listBranchesCmd = fGerritClient.listBranches(fChangeInfo.getProject());

		BranchInfo[] listBranchesCmdResult = null;
		try {
			listBranchesCmdResult = listBranchesCmd.call();
		} catch (EGerritException e3) {
			EGerritCorePlugin.logError(fGerritClient.getRepository().formatGerritVersion() + e3.getMessage());
		}
		return listBranchesCmdResult;
	}

	/**
	 * Post the reply information to the Gerrit server
	 *
	 * @param reviewInput
	 */
	private void postReply(ReviewInput reviewInput) {
		SetReviewCommand reviewToEmit = fGerritClient.setReview(fChangeInfo.getId(), fChangeInfo.getCurrent_revision());
		reviewToEmit.setCommandInput(reviewInput);

		try {
			reviewToEmit.call();
			refreshStatus();
			filesTab.setInitPatchSet();
		} catch (EGerritException e1) {
			EGerritCorePlugin.logError(fGerritClient.getRepository().formatGerritVersion() + e1.getMessage());
		}
	}

	@Override
	public void setFocus() {
		headerSection.setFocus();
	}

	/**
	 * This is the entry point to fill the editor with the Change info and the gerrit server being used.
	 *
	 * @param GerritClient
	 *            gerritClient
	 * @param ChangeInfo
	 *            element
	 */
	private void setChangeInfo(GerritClient gerritClient, ChangeInfo element) {
		fGerritClient = gerritClient;

		if (fChangeInfo == null) {
			fChangeInfo = element;
		} else {
			//Refresh only potential modified fields, otherwise the data binding seems not to know which fields are modified
			fChangeInfo.set_number(element.get_number());
			fChangeInfo.setChange_id(element.getChange_id());
			fChangeInfo.setStatus(element.getStatus());
			fChangeInfo.setStarred(element.isStarred());
			fChangeInfo.setCreated(element.getCreated());
			fChangeInfo.setUpdated(element.getUpdated());
			fChangeInfo.setReviewed(element.isReviewed());
			fChangeInfo.setMergeable(element.isMergeable());
			fChangeInfo.setInsertions(element.getInsertions());
			fChangeInfo.setDeletions(element.getDeletions());
			fChangeInfo.setCurrent_revision(element.getCurrent_revision());
		}
	}

	private int findMaxDefinedLabelValue(String label) {
		Iterator<Entry<String, LabelInfo>> iterator = fChangeInfo.getLabels().entrySet().iterator();
		//Get the structure having all the possible options
		int maxDefined = 0;
		while (iterator.hasNext()) {
			Entry<String, LabelInfo> definedlabel = iterator.next();
			if (definedlabel.getKey().compareTo(label) == 0) {
				for (String element2 : definedlabel.getValue().getValues().keySet()) {
					maxDefined = Math.max(maxDefined, new Integer(element2.trim()));
				}
			}
		}
		return maxDefined;
	}

	private int findMaxPermitted(String label) {
		int maxPermitted = 0;
		EList<String> listPermitted = null;
		EMap<String, EList<String>> mapLabels = fChangeInfo.getPermitted_labels();
		if (mapLabels != null) {
			Iterator<Entry<String, EList<String>>> iterator = mapLabels.entrySet().iterator();
			//Get the structure having all the possible options
			while (iterator.hasNext()) {
				Entry<String, EList<String>> permittedlabel = iterator.next();
				listPermitted = permittedlabel.getValue();
				if (permittedlabel.getKey().compareTo(label) == 0) {
					for (String element2 : listPermitted) {
						maxPermitted = Math.max(maxPermitted, new Integer(element2.trim()));
					}
				}
			}
		}
		return maxPermitted;
	}

	/**
	 * Refreshes the ChangeEditor
	 */
	public void refreshStatus() {
		ChangeInfo changeInfo = QueryHelpers.lookupPartialChangeInfoFromChangeId(fGerritClient, fChangeInfo.getId(),
				new NullProgressMonitor());

		//Reset specific fields, not the whole model structure
		setChangeInfo(fGerritClient, changeInfo);

		//This query fills the current revision
		setCurrentRevisionAndMessageTab(fGerritClient, fChangeInfo.getId());

		//Queries to fill the Summary Review tab data
		summaryTab.setTabs(fGerritClient, fChangeInfo);

		buttonsEnablement();

		LinkDashboard linkDash = new LinkDashboard(fGerritClient);
		linkDash.invokeRefreshDashboardCommand("", ""); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/**
	 * The logic that controls when bottom buttons are enabled or not
	 */
	private void buttonsEnablement() {
		submitrevertButtonEnablement();
		abandonrestoreButtonEnablement();
		rebaseButtonEnablement();
		draftButtonEnablement();
	}

	private void draftButtonEnablement() {

		fDraftPublishDelete.setEnabled(fChangeInfo.getStatus().compareTo("DRAFT") == 0 && canDeleteDraft());

	}

	private void rebaseButtonEnablement() {
		fRebase.setEnabled(canRebase());

	}

	private void submitrevertButtonEnablement() {

		if (canSubmit()) {
			fSubmitRevert.setEnabled(true);
			fSubmitMode = true;
		} else if (canRevert()) {
			fSubmitRevert.setEnabled(true);
			fSubmitMode = false;
		} else {
			fSubmitRevert.setEnabled(false);
		}

		//Adjust the text on the button
		if (fSubmitMode) {
			fSubmitRevert.setText("Submit");
		} else {
			fSubmitRevert.setText("Revert");
		}

	}

	private boolean canDeleteDraft() {
		EMap<String, ActionInfo> actions = getRevisionActions();

		if (actions != null) {
			ActionInfo delete = actions.get("publish");

			if (delete != null) {
				return delete.isEnabled();
			}
		}
		return false;
	}

	private boolean canSubmit() {
		EMap<String, ActionInfo> actions = getRevisionActions();
		if (actions != null) {
			ActionInfo submit = actions.get(SUBMIT);

			if (submit != null) {
				return submit.isEnabled();
			}
		}
		return false;
	}

	private boolean canRevert() {
		EMap<String, ActionInfo> actions = fChangeInfo.getActions();
		if (actions != null) {
			ActionInfo revert = actions.get(REVERT);
			if (revert != null) {
				if (revert.isEnabled()) {
					return true;
				}
			}

		}
		return false;
	}

	private boolean canRebase() {
		EMap<String, ActionInfo> actions = getRevisionActions();
		if (actions != null) {
			ActionInfo rebase = actions.get(REBASE);

			if (rebase != null) {
				return true;
			}
		}
		return false;
	}

	//EMF this logic is weird...
	private EMap<String, ActionInfo> getRevisionActions() {
		//Test if we already have the info in the revision info structure, we should
		EMap<String, RevisionInfo> maprev = fChangeInfo.getRevisions();
		if (maprev != null) {
			return maprev.get(fChangeInfo.getCurrent_revision()).getActions();
		}

		//Query if the action for a revision are not available yet Should not happen here
		GetRevisionActionsCommand getRevisionActionsCmd = fGerritClient.getRevisionActions(fChangeInfo.getId(),
				fChangeInfo.getCurrent_revision());

		Map<String, ActionInfo> getRevisionActionsCmdResult = null;
		try {
			getRevisionActionsCmdResult = getRevisionActionsCmd.call();
			fChangeInfo.getRevisions().get(fChangeInfo.getCurrent_revision()).eSet(
					ModelPackage.Literals.REVISION_INFO__ACTIONS, getRevisionActionsCmdResult);
		} catch (EGerritException e3) {
			EGerritCorePlugin.logError(fGerritClient.getRepository().formatGerritVersion() + e3.getMessage());
		}
		return fChangeInfo.getRevisions().get(fChangeInfo.getCurrent_revision()).getActions();
	}

	private void abandonrestoreButtonEnablement() {

		EMap<String, ActionInfo> actions = fChangeInfo.getActions();
		if (actions != null) {
			ActionInfo abandon = actions.get(ABANDON);
			ActionInfo restore = actions.get(RESTORE);
			fAbandonRestore.setText("Abandon");
			fAbandonRestore.setEnabled(true);

			if (abandon != null && abandon.isEnabled()) {
				fAbandonMode = true;
			} else if (restore != null && restore.isEnabled()) {
				fAbandonRestore.setText("Restore");
			} else {
				fAbandonRestore.setEnabled(false);
			}

		}
	}

	/************************************************************* */
	/*                                                             */
	/* Section to SET the data structure                           */
	/*                                                             */
	/************************************************************* */

	/**
	 * Fill the data related to the current revision, labels and messages. Also, the data for the Message tab is
	 * available with this request
	 *
	 * @param GerritClient
	 *            gerritClient
	 * @param String
	 *            change_id
	 */
	private void setCurrentRevisionAndMessageTab(GerritClient gerritClient, String id) {
		ChangeInfo res = queryMessageTab(gerritClient, id, new NullProgressMonitor());

		if (res != null) {
			fChangeInfo.setCurrent_revision(res.getCurrent_revision());
			fChangeInfo.getLabels().clear();
			fChangeInfo.getLabels().addAll(res.getLabels());
			fChangeInfo.getMessages().clear();
			fChangeInfo.getMessages().addAll(res.getMessages());
			fChangeInfo.getPermitted_labels().clear();
			fChangeInfo.getPermitted_labels().addAll(res.getPermitted_labels());
			fChangeInfo.setStatus(res.getStatus());
			fChangeInfo.setSubject(res.getSubject());
			fChangeInfo.getActions().clear();
			fChangeInfo.getActions().addAll(res.getActions());
			fChangeInfo.setTopic(res.getTopic());
			fChangeInfo.setMergeable(res.isMergeable());
			fChangeInfo.getRevisions().clear();
			fChangeInfo.getRevisions().addAll(res.getRevisions());

		}

	}

	/***************************************************************/
	/*                                                             */
	/* Section to QUERY the data structure                         */
	/*                                                             */
	/************************************************************* */

	private ChangeInfo queryMessageTab(GerritClient gerrit, String id, IProgressMonitor monitor) {
		try {
			monitor.beginTask("Executing query", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
			// Create query

			GetChangeCommand command = gerrit.getChange(id);
			command.addOption(ChangeOption.DETAILED_LABELS);
			command.addOption(ChangeOption.ALL_FILES);
			command.addOption(ChangeOption.ALL_REVISIONS);
			command.addOption(ChangeOption.ALL_COMMITS);
			command.addOption(ChangeOption.REVIEWED);
			command.addOption(ChangeOption.MESSAGES);
			command.addOption(ChangeOption.DOWNLOAD_COMMANDS);
			command.addOption(ChangeOption.CURRENT_ACTIONS);

			ChangeInfo res = null;
			try {
				res = command.call();
			} catch (EGerritException e) {
				EGerritCorePlugin.logError(gerrit.getRepository().formatGerritVersion() + e.getMessage());
			}
			return res;
		} finally {
			monitor.done();
		}
	}

	/************************************************************* */
	/*                                                             */
	/* Section adjust the DATA binding                             */
	/*                                                             */
	/************************************************************* */

	protected DataBindingContext headerSectionDataBindings() {
		DataBindingContext bindingContext = new DataBindingContext();

		//Show id
		IObservableValue idFChangeInfoObserveValue = EMFProperties.value(ModelPackage.Literals.CHANGE_INFO__NUMBER)
				.observe(fChangeInfo);
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setGroupingUsed(false);
		bindingContext.bindValue(WidgetProperties.text().observe(shortIdData), idFChangeInfoObserveValue, null,
				new UpdateValueStrategy().setConverter(NumberToStringConverter.fromInteger(numberFormat, true)));

		//Show subject
		IObservableValue changeIdFChangeInfoObserveValue = EMFProperties
				.value(ModelPackage.Literals.CHANGE_INFO__SUBJECT).observe(fChangeInfo);
		bindingContext.bindValue(WidgetProperties.text().observe(subjectData), changeIdFChangeInfoObserveValue, null,
				null);

		//Show status
		IObservableValue statusObserveValue = EMFProperties.value(ModelPackage.Literals.CHANGE_INFO__STATUS)
				.observe(fChangeInfo);
		bindingContext.bindValue(WidgetProperties.text().observe(statusData), statusObserveValue, null, null);

		return bindingContext;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// ignore
	}

	@Override
	public void doSaveAs() {
		// ignore
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof ChangeDetailEditorInput)) {
			System.err.println("Input is not a ChangeDetailEditorInput");
		}
		setSite(site);
		setInput(input);
		setChangeInfo(((ChangeDetailEditorInput) input).getClient(), ((ChangeDetailEditorInput) input).getChange());
	}

	@Override
	public boolean isDirty() {
		// ignore
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// ignore
		return false;
	}

	/*********************************************/
/*                                           */
/*       Buttons Listener                    */
/*                                           */
	/*********************************************/

	/**
	 * @param Compopsite
	 *            parent
	 * @return the listener when the Checkout button is triggered
	 */
	private SelectionAdapter checkoutButtonListener(final Composite parent) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Repository localRepo = findLocalRepo(fGerritClient, summaryTab.getProject());

				if (localRepo == null) {
					Status status = new Status(IStatus.ERROR, EGerritCorePlugin.PLUGIN_ID, "No repository found");
					ErrorDialog.openError(parent.getShell(), "Error", "Operation could not be performed", status);
					return;
				}

				String psSelected = filesTab.getSelectedPatchSet();
				if ((psSelected == null) || psSelected.isEmpty()) {
					Status status = new Status(IStatus.ERROR, EGerritCorePlugin.PLUGIN_ID, "No patchset selected");
					ErrorDialog.openError(parent.getShell(), "Error", "Operation could not be performed", status);
				}

				//Find the current selected Patch set reference in the table
				FetchGerritChangeWizard var = new FetchGerritChangeWizard(localRepo, psSelected);
				WizardDialog w = new WizardDialog(parent.getShell(), var);
				String currentActiveBranchName = getCurrentBranchName(localRepo);
				w.create();
				String dialogErrorMsg = w.getErrorMessage();
				String existingLocalBranchName = getTargetBranchName(parent, localRepo);
				// already on the branch, nothing to do
				if (existingLocalBranchName != null
						&& currentActiveBranchName.compareTo(existingLocalBranchName) == 0) {
					return;
				}
				// the target branch exists
				if (dialogErrorMsg != null && dialogErrorMsg.contains("already exists")) { // branch already exists,

					int result = checkOutOrNot(existingLocalBranchName);
					if (result == 256) {
						w.open();
						return;
					} else if (result == Window.CANCEL) { // cancel
						return;
					}

					try {
						checkoutBranch(existingLocalBranchName, localRepo);
					} catch (Exception e) {
					}
				} else {
					// no branch yet, go through wizard
					try {
						w.open();

					} catch (Exception e) {
					}
				}
			}

		};

	}

	private String getCurrentBranchName(Repository localRepo) {
		String head = null;
		try {
			head = localRepo.getFullBranch();
		} catch (IOException e1) {

		}
		if (head.startsWith("refs/heads/")) { // Print branch name with "refs/heads/" stripped.
			try {
				return localRepo.getBranch();
			} catch (IOException e) {
			}

		}
		return head;
	}

	private String getTargetBranchName(final Composite parent, Repository localRepo) {
		Set<String> branches = null;
		try {
			branches = getShortLocalBranchNames(new Git(localRepo));
		} catch (GitAPIException e1) {
			Status status = new Status(IStatus.ERROR, EGerritCorePlugin.PLUGIN_ID, "No branches found");
			ErrorDialog.openError(parent.getShell(), "Error", "Operation could not be performed", status);
			return "";
		}
		String shortName = fChangeInfo.get_number() + "/" + filesTab.getSelectedPatchSetNumber();
		Iterator<String> iterator = branches.iterator();
		String branchName = null;
		while (iterator.hasNext()) {
			String setElement = iterator.next();
			if (setElement.contains(shortName)) {
				branchName = setElement;
				break;
			}
		}
		return branchName;
	}

	private int checkOutOrNot(String branchName) {
		Preferences prefs = ConfigurationScope.INSTANCE.getNode("org.eclipse.egerrit.prefs");

		Preferences editorPrefs = prefs.node("checkout");

		int choice = editorPrefs.getInt("doCheckout", 0);

		if (choice != 0) {
			return choice;
		}
		String title = "Review previously checked-out";
		String message = "Change \"" + fChangeInfo.getSubject() + "\"" + " has previously been checked out in branch "
				+ "\"" + branchName + "\"" + ".\n\n" + "Do you want to switch to it or create a new branch?";
		MessageDialogWithToggle dialog = new MessageDialogWithToggle(fDraftPublishDelete.getParent().getShell(), title,
				null, message, MessageDialog.NONE, new String[] { "Cancel", "New Branch", "Switch" }, 0,
				"Always perform this action", false);
		dialog.open();
		int result = dialog.getReturnCode();

		if (result != Window.CANCEL && dialog.getToggleState()) {
			editorPrefs.putInt("doCheckout", result);
			try {
				editorPrefs.flush();
			} catch (BackingStoreException e) {
				//There is not much we can do
			}
		}
		return result;
	}

	private static Set<String> getShortLocalBranchNames(Git git) throws GitAPIException {
		Set<String> branches = new HashSet<String>();
		Iterator<Ref> iter = git.branchList().call().iterator();
		while (iter.hasNext()) {
			branches.add(Repository.shortenRefName(iter.next().getName()));
		}
		return branches;
	}

	private void checkoutBranch(String branchName, Repository repo) throws Exception {
		CheckoutCommand command = null;
		try {
			command = new Git(repo).checkout();
			command.setCreateBranch(false);
			command.setName(branchName);
			command.setForce(false);
			command.call();
		} catch (Throwable t) {
			CheckoutResult result = command.getResult();
			new CheckoutConflictDialog(fDraftPublishDelete.getParent().getShell(), repo, result.getConflictList())
					.open();
		}
	}

	/*********************************************/
/*                                           */
/*       Utility                             */
/*                                           */
	/*********************************************/

	/**
	 * @param GerritClient
	 *            gerritClient
	 * @param String
	 *            projectName
	 * @return Repository The local repository associated to the current project and Gerrit Repository
	 */
	private Repository findLocalRepo(GerritClient gerrit, String projectName) {
		GerritToGitMapping gerritToGitMap = null;
		try {
			gerritToGitMap = new GerritToGitMapping(new URIish(gerrit.getRepository().getURIBuilder(false).toString()),
					projectName);
		} catch (URISyntaxException e2) {
			EGerritCorePlugin.logError(gerrit.getRepository().formatGerritVersion() + e2.getMessage());
		}
		Repository jgitRepo = null;
		try {
			jgitRepo = gerritToGitMap.find();
		} catch (IOException e2) {
			EGerritCorePlugin.logError(gerrit.getRepository().formatGerritVersion() + e2.getMessage());
		}
		return jgitRepo;
	}

	/**
	 * @param Observable
	 * @param Object
	 */
	@Override
	//EMF this should go away
	public void update(Observable arg0, Object arg1) {
		if (arg0 instanceof MessageTabView) {
			//in this case, we need to update the Subject. The following request do more
			//but at least, less than the whole refresh
			setCurrentRevisionAndMessageTab(fGerritClient, fChangeInfo.getId());
			setEditorTitle();
		}
	}

	private void setEditorTitle() {
		//Change the window title if needed
		if (fChangeInfo != null) {
			setPartName(fChangeInfo.getSubject());
		}

	}

	/**
	 * @param PropertyChangeEvent
	 *            evt
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// ignore
	}

	public void invokeRefreshCommand() {
		// Obtain IServiceLocator implementer, e.g. from PlatformUI.getWorkbench():
		IServiceLocator serviceLocator = PlatformUI.getWorkbench();
		// or a site from within a editor or view:
		// IServiceLocator serviceLocator = getSite();

		ICommandService commandService = serviceLocator.getService(ICommandService.class);

		try {
			// Lookup command with its ID
			Command command = commandService.getCommand("org.eclipse.egerrit.dashboard.refresh");

			// Optionally pass a ExecutionEvent instance, default no-param arg creates blank event
			command.executeWithChecks(new ExecutionEvent());

		} catch (NotDefinedException | NotEnabledException | NotHandledException
				| org.eclipse.core.commands.ExecutionException e) {

			EGerritCorePlugin.logError(fGerritClient.getRepository().formatGerritVersion() + e.getMessage());

		}
	}

	/**
	 * Open another editor after a revert action with the newest changeInfo
	 *
	 * @param changeInfo
	 */
	private void openAnotherEditor(ChangeInfo changeInfo) {
		IWorkbench workbench = EGerritUIPlugin.getDefault().getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page = null;
		if (window != null) {
			page = workbench.getActiveWorkbenchWindow().getActivePage();
		}

		if (page != null) {
			try {
				IEditorInput input = new ChangeDetailEditorInput(fGerritClient, changeInfo);
				IEditorPart reusedEditor = page.findEditor(input);
				page.openEditor(input, ChangeDetailEditor.EDITOR_ID);
				if (reusedEditor instanceof ChangeDetailEditor) {
					((ChangeDetailEditor) reusedEditor).refreshStatus();
				}
			} catch (PartInitException e) {
				EGerritCorePlugin.logError(fGerritClient != null
						? fGerritClient.getRepository().formatGerritVersion() + e.getMessage()
						: e.getMessage());
			}
		}
	}

}