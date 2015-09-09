/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Guy Perron - Initial API and implementation
 *******************************************************************************/

package org.eclipse.egerrit.core.command;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.egerrit.core.EGerritCorePlugin;
import org.eclipse.egerrit.core.GerritRepository;
import org.eclipse.egerrit.core.rest.CommentInfo;

import com.google.gson.Gson;

/**
 * The command: PUT
 * /changes/link:#change-id[\{change-id\}]/revisions/link:#revision-id[\{revision-id\}]/drafts/[\{draft-id\}] result a
 * CommentInfo.
 * <p>
 *
 * @since 1.0
 */

public class UpdateDraftCommand extends PutCommand<CommentInfo> {

	// ------------------------------------------------------------------------
	// Attributes
	// ------------------------------------------------------------------------

	private String fChange_id;

	private String fRevision;

	private String fDraft;

	private CommentInfo fCommentInfo = new CommentInfo();

	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------

	/**
	 * The constructor
	 *
	 * @param gerritRepository
	 *            the gerrit repository
	 * @param id
	 *            the change-id
	 * @param revision
	 *            revisions-id
	 */
	public UpdateDraftCommand(GerritRepository gerritRepository, String id, String revision, String draft) {
		super(gerritRepository, CommentInfo.class);
		this.setId(id);
		this.setRevision(revision);
		this.setDraft(draft);
		requiresAuthentication(true);
	}

	private void setRevision(String revision) {
		fRevision = revision;

	}

	private String getRevision() {
		return fRevision;

	}

	/**
	 * change_id getter function
	 *
	 * @return change_id the current change_id
	 */
	private String getId() {
		return fChange_id;
	}

	/**
	 * change_id setter function
	 *
	 * @param change_id
	 *            the change_id of the comment that is to be created
	 */

	private void setId(String change_id) {
		this.fChange_id = change_id;
	}

	/**
	 * fCommentInfo setter function
	 *
	 * @param commentInfo
	 *            the commentInfo that is to be created
	 */
	public void setCommentInfo(CommentInfo commentInfo) {

		fCommentInfo = commentInfo;

	}

	/**
	 * @return the fDraft
	 */
	private String getDraft() {
		return fDraft;
	}

	/**
	 * @param fDraft
	 *            the fDraft to set
	 */
	private void setDraft(String fDraft) {
		this.fDraft = fDraft;
	}

	// ------------------------------------------------------------------------
	// Format the query
	// ------------------------------------------------------------------------

	/*	 * (non-Javadoc)
	 *
	 * @see org.eclipse.egerrit.core.command.GerritCommand#formatRequest()
	 */
	@Override
	public HttpRequestBase formatRequest() {

		// Get the generic URI
		URIBuilder uriBuilder = getRepository().getURIBuilder(fAuthIsRequired);
		URI uri = null;
		try {
			// Set the path
			String path = new StringBuilder(uriBuilder.getPath()).append("/changes/") //$NON-NLS-1$
					.append(getId())
					.append("/revisions/") //$NON-NLS-1$
					.append(getRevision())
					.append("/drafts/") //$NON-NLS-1$
					.append(getDraft())
					.toString();
			uriBuilder.setPath(path);
			uri = new URI(URIUtil.toUnencodedString(uriBuilder.build()));
		} catch (URISyntaxException e) {
			EGerritCorePlugin.logError("URI syntax exception", e); //$NON-NLS-1$
		}

		HttpPut httpPut = new HttpPut(uri);

		StringEntity input = null;
		try {
			input = new StringEntity(new Gson().toJson(fCommentInfo));
		} catch (UnsupportedEncodingException e) {
			EGerritCorePlugin.logError("URI error encoding CommentInfo", e); //$NON-NLS-1$
		}

		if (input != null) {
			input.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, GerritCommand.JSON_HEADER));
		}
		httpPut.setEntity(input);

		return httpPut;
	}
}