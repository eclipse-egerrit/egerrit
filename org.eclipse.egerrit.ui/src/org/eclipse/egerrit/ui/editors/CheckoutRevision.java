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

package org.eclipse.egerrit.ui.editors;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.egerrit.core.EGerritCorePlugin;
import org.eclipse.egerrit.core.GerritClient;
import org.eclipse.egerrit.internal.model.ChangeInfo;
import org.eclipse.egerrit.internal.model.RevisionInfo;
import org.eclipse.egerrit.ui.internal.utils.ActiveWorkspaceRevision;
import org.eclipse.egerrit.ui.internal.utils.GerritToGitMapping;
import org.eclipse.egerrit.ui.internal.utils.Messages;
import org.eclipse.egit.ui.internal.dialogs.CheckoutConflictDialog;
import org.eclipse.egit.ui.internal.fetch.FetchGerritChangeWizard;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class CheckoutRevision extends Action {

	private RevisionInfo revision;

	private GerritClient gerritClient;

	private ChangeInfo changeInfo;

	public CheckoutRevision(RevisionInfo revision, GerritClient gerritClient) {
		this.revision = revision;
		this.gerritClient = gerritClient;
		this.changeInfo = this.revision.getChangeInfo();
		setText(Messages.CheckoutRevision_0);
	}

	@Override
	public void run() {
		Repository localRepo = findLocalRepo(gerritClient, changeInfo.getProject());

		if (localRepo == null) {
			Status status = new Status(IStatus.ERROR, EGerritCorePlugin.PLUGIN_ID, Messages.CheckoutRevision_1);
			ErrorDialog.openError(getShell(), Messages.CheckoutRevision_2, Messages.CheckoutRevision_3, status);
			return;
		}

		String psSelected = revision.getRef();
		if ((psSelected == null) || psSelected.isEmpty()) {
			Status status = new Status(IStatus.ERROR, EGerritCorePlugin.PLUGIN_ID, Messages.CheckoutRevision_4);
			ErrorDialog.openError(getShell(), Messages.CheckoutRevision_2, Messages.CheckoutRevision_3, status);
		}

		//Find the current selected Patch set reference in the table
		FetchGerritChangeWizard var = new FetchGerritChangeWizard(localRepo, psSelected);
		WizardDialog w = new WizardDialog(getShell(), var);
		String currentActiveBranchName = getCurrentBranchName(localRepo);
		w.create();
		String dialogErrorMsg = w.getErrorMessage();
		String existingLocalBranchName = getTargetBranchName(localRepo);
		// already on the branch, nothing to do
		if (existingLocalBranchName != null && currentActiveBranchName.compareTo(existingLocalBranchName) == 0) {
			ActiveWorkspaceRevision.getInstance().activateCurrentRevision(gerritClient,
					changeInfo.getUserSelectedRevision());
			return;
		}

		// the target branch exists
		if (dialogErrorMsg != null && dialogErrorMsg.contains("already exists")) { // branch already exists, //$NON-NLS-1$
			int result = checkOutOrNot(existingLocalBranchName);
			if (result == IDialogConstants.INTERNAL_ID) {
				w.open();
				return;
			} else if (result == IDialogConstants.CANCEL_ID) { // cancel
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
		ActiveWorkspaceRevision.getInstance().activateCurrentRevision(gerritClient,
				changeInfo.getUserSelectedRevision());
	}

	private String getCurrentBranchName(Repository localRepo) {
		String head = null;
		try {
			head = localRepo.getFullBranch();
		} catch (IOException e1) {

		}
		if (head.startsWith(Messages.CheckoutRevision_7)) { // Print branch name with "refs/heads/" stripped.
			try {
				return localRepo.getBranch();
			} catch (IOException e) {
			}

		}
		return head;
	}

	private int checkOutOrNot(String branchName) {
		Preferences prefs = ConfigurationScope.INSTANCE.getNode("org.eclipse.egerrit.prefs"); //$NON-NLS-1$

		Preferences editorPrefs = prefs.node("checkout"); //$NON-NLS-1$

		int choice = editorPrefs.getInt("doCheckout", 0); //$NON-NLS-1$

		if (choice != 0) {
			return choice;
		}
		String title = Messages.CheckoutRevision_11;
		String message = Messages.CheckoutRevision_12 + changeInfo.getSubject() + Messages.CheckoutRevision_13
				+ Messages.CheckoutRevision_14 + Messages.CheckoutRevision_15 + branchName
				+ Messages.CheckoutRevision_16 + Messages.CheckoutRevision_17 + Messages.CheckoutRevision_18;
		MessageDialogWithToggle dialog = new MessageDialogWithToggle(Display.getDefault().getActiveShell(), title,
				null, message, MessageDialog.NONE, new String[] { Messages.CheckoutRevision_19,
						Messages.CheckoutRevision_20, Messages.CheckoutRevision_21 },
				0, Messages.CheckoutRevision_22, false);
		dialog.open();
		int result = dialog.getReturnCode();

		if (result != IDialogConstants.CANCEL_ID && dialog.getToggleState()) {
			editorPrefs.putInt("doCheckout", result); //$NON-NLS-1$
			try {
				editorPrefs.flush();
			} catch (BackingStoreException e) {
				//There is not much we can do
			}
		}
		return result;
	}

	private void checkoutBranch(String branchName, Repository repo) throws Exception {
		CheckoutCommand command = null;
		try (Git gitRepo = new Git(repo)) {
			command = gitRepo.checkout();
			command.setCreateBranch(false);
			command.setName(branchName);
			command.setForce(false);
			command.call();
		} catch (Throwable t) {
			CheckoutResult result = command.getResult();
			new CheckoutConflictDialog(Display.getDefault().getActiveShell(), repo, result.getConflictList()).open();
		}
	}

	private String getTargetBranchName(Repository localRepo) {
		Set<String> branches = null;
		try {
			branches = getShortLocalBranchNames(new Git(localRepo));
		} catch (GitAPIException e1) {
			Status status = new Status(IStatus.ERROR, EGerritCorePlugin.PLUGIN_ID, Messages.CheckoutRevision_24);
			ErrorDialog.openError(getShell(), Messages.CheckoutRevision_2, Messages.CheckoutRevision_26, status);
			return Messages.CheckoutRevision_27;
		}
		String shortName = changeInfo.get_number() + "/" + changeInfo.getUserSelectedRevision().get_number(); //$NON-NLS-1$
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

	private Set<String> getShortLocalBranchNames(Git git) throws GitAPIException {
		Set<String> branches = new HashSet<String>();
		Iterator<Ref> iter = git.branchList().call().iterator();
		while (iter.hasNext()) {
			branches.add(Repository.shortenRefName(iter.next().getName()));
		}
		return branches;
	}

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

	private Shell getShell() {
		return Display.getDefault().getActiveShell();
	}
}
