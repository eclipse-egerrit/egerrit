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

import java.text.SimpleDateFormat;

import org.apache.http.client.ClientProtocolException;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.egerrit.core.EGerritCorePlugin;
import org.eclipse.egerrit.core.GerritClient;
import org.eclipse.egerrit.core.command.ChangeCommitMsgCommand;
import org.eclipse.egerrit.core.command.PublishChangeEditCommand;
import org.eclipse.egerrit.core.exception.EGerritException;
import org.eclipse.egerrit.core.rest.ChangeEditMessageInput;
import org.eclipse.egerrit.core.rest.ChangeInfo;
import org.eclipse.egerrit.core.rest.CommitInfo;
import org.eclipse.egerrit.ui.internal.utils.DataConverter;
import org.eclipse.egerrit.ui.internal.utils.UIUtils;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 * This class is used in the editor to handle the Gerrit message view
 *
 * @since 1.0
 */
public class MessageTabView {

	private Text msgTextData;

	private Label msgAuthorData;

	private Label msgDatePushData;

	private Label msgCommitterData;

	private Label msgDatecommitterData;

	private Label msgCommitidData;

	private Label msgParentIdData;

	private Label msgChangeIdData;

	private final SimpleDateFormat formatTimeOut = new SimpleDateFormat("MMM d, yyyy  hh:mm a"); //$NON-NLS-1$

	private final String TITLE = "Gerrit Server ";

	private Button fBtnSave;

	private boolean fIsEditingAllowed;

	private String fMessageBuffer;

	// ------------------------------------------------------------------------
	// Constructor and life cycle
	// ------------------------------------------------------------------------

	/**
	 * The constructor.
	 */
	public MessageTabView() {
	}

	/**
	 * @param fGerritClient
	 * @param tabFolder
	 * @param listMessages
	 *            List<ChangeMessageInfo>
	 */
	public void create(GerritClient fGerritClient, TabFolder tabFolder, CommitInfo commitInfo, ChangeInfo changeInfo) {
		messagesTab(fGerritClient, tabFolder, commitInfo, changeInfo);
	}

	private void messagesTab(final GerritClient fGerritClient, TabFolder tabFolder, CommitInfo commitInfo,
			final ChangeInfo changeInfo) {

		Point fontSize = UIUtils.computeFontSize(tabFolder);
		final TabItem tabMessages = new TabItem(tabFolder, SWT.NONE);
		tabMessages.setText("Messages");
		final ScrolledComposite sc_msg = new ScrolledComposite(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		sc_msg.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		sc_msg.setExpandHorizontal(true);
		sc_msg.setExpandVertical(true);
		final Group messagesGroup = new Group(sc_msg, SWT.NONE);

		tabMessages.setControl(sc_msg);
		GridLayout gl_messagesGroup = new GridLayout(4, false);
		gl_messagesGroup.horizontalSpacing = 10;
		gl_messagesGroup.marginTop = 3;
		gl_messagesGroup.marginBottom = 3;
		messagesGroup.setLayout(gl_messagesGroup);

		ScrolledComposite sc_msgtxt = new ScrolledComposite(messagesGroup, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		sc_msgtxt.setExpandHorizontal(true);
		sc_msgtxt.setExpandVertical(true);
		sc_msgtxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		msgTextData = new Text(sc_msgtxt, SWT.BORDER | SWT.WRAP | SWT.MULTI);
		sc_msgtxt.setContent(msgTextData);
		int minHeight = fontSize.y * 6; //minimum 6 lines
		sc_msgtxt.setMinSize(msgTextData.computeSize(SWT.DEFAULT, minHeight));

		Composite composite = new Composite(messagesGroup, SWT.NONE);
		GridLayout gl_composite = new GridLayout(3, false);
		gl_composite.marginTop = 3;
		composite.setLayout(gl_composite);
		GridData gd_composite = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_composite.minimumHeight = 117;
		composite.setLayoutData(gd_composite);

		Label lblAuthor = new Label(composite, SWT.NONE);
		lblAuthor.setText("Author:");

		msgAuthorData = new Label(composite, SWT.NONE);
//		msgAuthorData = new StyledText(composite, SWT.NONE);
		msgAuthorData.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		msgDatePushData = new Label(composite, SWT.NONE);
		GridData gd_DatePush = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		int maxDatePush = 22; //Max number of chars
		gd_DatePush.widthHint = fontSize.x * maxDatePush;
		msgDatePushData.setLayoutData(gd_DatePush);

		Label lblCommitter = new Label(composite, SWT.NONE);
		lblCommitter.setText("Committer:");

		msgCommitterData = new Label(composite, SWT.NONE);
		msgCommitterData.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		msgDatecommitterData = new Label(composite, SWT.NONE);
		GridData gd_DateCommitter = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
		int maxDateCommit = 22; //Max number of chars
		gd_DateCommitter.widthHint = fontSize.x * maxDateCommit;
		msgDatecommitterData.setLayoutData(gd_DateCommitter);

		Label lblCommit = new Label(composite, SWT.NONE);
		lblCommit.setText("Commit:");

		msgCommitidData = new Label(composite, SWT.NONE);
		msgCommitidData.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(composite, SWT.NONE);

		Label lblParents = new Label(composite, SWT.NONE);
		lblParents.setText("Parent(s):");

		msgParentIdData = new Label(composite, SWT.NONE);
		msgParentIdData.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		new Label(composite, SWT.NONE);

		Label lblChangeid = new Label(composite, SWT.NONE);
		lblChangeid.setText("Change-Id:");

		msgChangeIdData = new Label(composite, SWT.NONE);
		msgChangeIdData.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		final Button btnCancel = new Button(messagesGroup, SWT.NONE);
		GridData gd_btnCancel = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_btnCancel.verticalIndent = 10;
		btnCancel.setLayoutData(gd_btnCancel);
		btnCancel.setText("Cancel");
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				msgTextData.setText(getfMessageBuffer());
				fBtnSave.setEnabled(false);
				btnCancel.setEnabled(false);
			}
		});

		fBtnSave = new Button(messagesGroup, SWT.NONE);
		GridData gd_btnSave_1 = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
		gd_btnSave_1.verticalIndent = 10;
		fBtnSave.setLayoutData(gd_btnSave_1);
		fBtnSave.setText("Save");

		ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				fBtnSave.setEnabled(isEditingAllowed());
				btnCancel.setEnabled(isEditingAllowed());
			}
		};

		FocusListener focusListener = new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				msgTextData.setEditable(isEditingAllowed());
			}

			@Override
			public void focusLost(FocusEvent e) {

			}

		};

		msgTextData.addModifyListener(modifyListener);
		msgTextData.addFocusListener(focusListener);

		fBtnSave.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				ChangeCommitMsgCommand editMessageCmd = fGerritClient.editMessage(changeInfo.getId());
				ChangeEditMessageInput changeEditMessageInput = new ChangeEditMessageInput();
				changeEditMessageInput.setMessage(msgTextData.getText());
				editMessageCmd.setChangeEditMessageInput(changeEditMessageInput);
				PublishChangeEditCommand publishChangeEditCmd = fGerritClient.publishChangeEdit(changeInfo.getId());

				String editMessageCmdResult = null, publishChangeEditCmdResult = null;
				try {
					editMessageCmdResult = editMessageCmd.call();
					publishChangeEditCmdResult = publishChangeEditCmd.call();
				} catch (EGerritException e3) {
					EGerritCorePlugin.logError(e3.getMessage());
				} catch (ClientProtocolException e3) {
					UIUtils.displayInformation(null, TITLE,
							e3.getLocalizedMessage() + "\n " + editMessageCmd.formatRequest()); //$NON-NLS-1$
				}
				fBtnSave.setEnabled(false);
				btnCancel.setEnabled(false);
				setfMessageBuffer(msgTextData.getText());

			}
		});

		new Label(messagesGroup, SWT.NONE);

		sc_msg.setContent(messagesGroup);

		//Set the binding for this section
		msgTabDataBindings(commitInfo, changeInfo);
	}

	protected DataBindingContext msgTabDataBindings(CommitInfo commitInfo, ChangeInfo changeInfo) {
		DataBindingContext bindingContext = new DataBindingContext();

		//
		IObservableValue observeMsgTextDataWidget = WidgetProperties.text().observe(msgTextData);
		IObservableValue msgTextDataValue = BeanProperties.value("message").observe(commitInfo); //$NON-NLS-1$
		bindingContext.bindValue(observeMsgTextDataWidget, msgTextDataValue, null, null);
		//
		IObservableValue observeTextMsgAuthorDataWidget = WidgetProperties.text().observe(msgAuthorData);
		IObservableValue msgAuthorDataValue = BeanProperties.value("author").observe(commitInfo); //$NON-NLS-1$
		bindingContext.bindValue(observeTextMsgAuthorDataWidget, msgAuthorDataValue, null,
				new UpdateValueStrategy().setConverter(DataConverter.gitPersonConverter()));

		//
		IObservableValue TextMsgAuthorDateDataWidget = WidgetProperties.text().observe(msgDatePushData);
		IObservableValue msgAuthorDateDataValue = BeanProperties.value(CommitInfo.class, "author.date").observe( //$NON-NLS-1$
				commitInfo);
		bindingContext.bindValue(TextMsgAuthorDateDataWidget, msgAuthorDateDataValue, null,
				new UpdateValueStrategy().setConverter(DataConverter.gerritTimeConverter(formatTimeOut)));

		//
		IObservableValue observeTextMsgCommitterDataWidget = WidgetProperties.text().observe(msgCommitterData);
		IObservableValue msgCommitterDataValue = BeanProperties.value("committer").observe(commitInfo); //$NON-NLS-1$
		bindingContext.bindValue(observeTextMsgCommitterDataWidget, msgCommitterDataValue, null,
				new UpdateValueStrategy().setConverter(DataConverter.gitPersonConverter()));

		//
		IObservableValue TextMsgCommitterDateDataWidget = WidgetProperties.text().observe(msgDatecommitterData);
		IObservableValue msgCommitterDateDataValue = BeanProperties.value(CommitInfo.class, "committer.date").observe( //$NON-NLS-1$
				commitInfo);
		bindingContext.bindValue(TextMsgCommitterDateDataWidget, msgCommitterDateDataValue, null,
				new UpdateValueStrategy().setConverter(DataConverter.gerritTimeConverter(formatTimeOut)));

		//
		IObservableValue observeTextMsgCommitidDataWidget = WidgetProperties.text().observe(msgCommitidData);
		IObservableValue msgCommitidDataValue = BeanProperties.value("commit").observe(commitInfo); //$NON-NLS-1$
		bindingContext.bindValue(observeTextMsgCommitidDataWidget, msgCommitidDataValue, null, null);
		//
		IObservableValue observeTextMsgParentIdDataWidget = WidgetProperties.text().observe(msgParentIdData);
		IObservableValue msgParentIdDataValue = BeanProperties.value("parents").observe(commitInfo); //$NON-NLS-1$
		bindingContext.bindValue(observeTextMsgParentIdDataWidget, msgParentIdDataValue, null,
				new UpdateValueStrategy().setConverter(DataConverter.commitInfoIDConverter()));
		//
		IObservableValue observeTextMsgChangeIdDataWidget = WidgetProperties.text().observe(msgChangeIdData);
		IObservableValue msgChangeIdDataValue = BeanProperties.value("change_id").observe(changeInfo); //$NON-NLS-1$
		bindingContext.bindValue(observeTextMsgChangeIdDataWidget, msgChangeIdDataValue, null, null);

		return bindingContext;
	}

	/**
	 * Enable editing on this screen for the current session
	 *
	 * @param isEditingAllowed
	 *            whether editing is enabled or not
	 */
	public void editingAllowed(boolean isEditingAllowed) {
		fIsEditingAllowed = isEditingAllowed;

	}

	private boolean isEditingAllowed() {
		return fIsEditingAllowed;
	}

	/**
	 * @return the fMessageBuffer
	 */
	public String getfMessageBuffer() {
		return fMessageBuffer;
	}

	/**
	 * @param fMessageBuffer
	 *            the message to set to save a buffer
	 */
	public void setfMessageBuffer(String fMessageBuffer) {
		this.fMessageBuffer = fMessageBuffer;
	}
}
