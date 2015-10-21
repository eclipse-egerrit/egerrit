/*******************************************************************************
 * Copyright (c) 2015 Ericsson AB.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson - initial API and implementation
 *******************************************************************************/

package org.eclipse.egerrit.ui.editors.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egerrit.core.EGerritCorePlugin;
import org.eclipse.egerrit.core.GerritClient;
import org.eclipse.egerrit.core.command.GetContentCommand;
import org.eclipse.egerrit.core.command.GetContentFromCommitCommand;
import org.eclipse.egerrit.core.command.ListCommentsCommand;
import org.eclipse.egerrit.core.command.ListDraftsCommand;
import org.eclipse.egerrit.core.exception.EGerritException;
import org.eclipse.egerrit.core.rest.CommentInfo;
import org.eclipse.egerrit.core.rest.FileInfo;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.AnnotationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class to create compare items
 */
public class CompareItemFactory {

	private Logger logger = LoggerFactory.getLogger(CompareItemFactory.class);

	private final static String TITLE = "Gerrit Server ";

	private GerritClient gerrit;

	private PatchSetCompareItem newCompareItem = new PatchSetCompareItem();

	public CompareItemFactory(GerritClient gerrit) {
		this.gerrit = gerrit;
	}

	/**
	 * Create an instance of a CompareItem
	 */
	public PatchSetCompareItem createCompareItem(String filename, String change_id, FileInfo fileInfo,
			IProgressMonitor monitor) {
		try {
			monitor.beginTask("Obtaining revision content", IProgressMonitor.UNKNOWN);

			newCompareItem.setFilename(filename);
			newCompareItem.setChange_id(change_id);
			newCompareItem.setGerritConnection(gerrit);
			newCompareItem.setRevision(fileInfo);

			String fileContent = null;

			// Create query
			GetContentCommand command = gerrit.getContent(change_id, fileInfo.getContainingRevisionInfo().getId(),
					filename);

			if (!"D".equals(fileInfo.getStatus())) { //$NON-NLS-1$
				try {
					fileContent = command.call();
					if (fileContent == null) {
						fileContent = ""; //$NON-NLS-1$
					}
				} catch (EGerritException e) {
					EGerritCorePlugin.logError(e.getMessage());
				}
			} else {
				fileContent = ""; //$NON-NLS-1$
			}
			Map<String, ArrayList<CommentInfo>> comments = loadComments(gerrit, change_id,
					fileInfo.getContainingRevisionInfo().getId());
			mergeCommentsInText(StringUtils.newStringUtf8(Base64.decodeBase64(fileContent)), comments.get(filename));

		} finally

		{
			monitor.done();
		}
		return newCompareItem;

	}

	/**
	 * Create a simple compare item
	 */
	public CommitCompareItem createSimpleCompareItem(String projectId, String commitId, String filePath,
			IProgressMonitor monitor) {
		GetContentFromCommitCommand getContent = gerrit.getContentFromCommit(projectId, commitId, filePath);
		String fileContent = ""; //$NON-NLS-1$
		try {
			fileContent = getContent.call();
		} catch (EGerritException e) {
			logger.debug("Exception retrieving commitId", e); //$NON-NLS-1$
		}
		return new CommitCompareItem(commitId, filePath, StringUtils.newStringUtf8(Base64.decodeBase64(fileContent)));
	}

	private Map<String, ArrayList<CommentInfo>> loadComments(GerritClient gerrit, String change_id,
			String revision_id) {
		Map<String, ArrayList<CommentInfo>> allComments = new HashMap<>();
		try {
			ListCommentsCommand getComments = gerrit.getListComments(change_id, revision_id);
			allComments = getComments.call();
			if (!gerrit.getRepository().getServerInfo().isAnonymous()) {
				ListDraftsCommand getDrafts = gerrit.listDraftsComments(change_id, revision_id);
				mergeComments(getDrafts.call(), allComments);
			}
		} catch (EGerritException e) {
			logger.debug("Exception retrieving commitId", e); //$NON-NLS-1$
		}

		return allComments;

	}

	private void mergeComments(Map<String, ArrayList<CommentInfo>> source, Map<String, ArrayList<CommentInfo>> target) {
		Set<Entry<String, ArrayList<CommentInfo>>> entries = source.entrySet();
		for (Entry<String, ArrayList<CommentInfo>> entry : entries) {
			ArrayList<CommentInfo> targetValue = target.get(entry.getKey());
			if (targetValue == null) {
				target.put(entry.getKey(), entry.getValue());
			} else {
				targetValue.addAll(entry.getValue());
			}
		}
	}

	//Take the original text and merge the comments into it
	//The insertion of comments starts from by last comment and proceed toward the first one. This allows for the insertion line to always be correct.
	private void mergeCommentsInText(String text, ArrayList<CommentInfo> comments) {
		//Create a document and an associated annotation model to keep track of the original text w/ comments
		AnnotationModel originalComments = new CommentAnnotationManager();
		Document originalDocument = new Document(text);
		originalDocument.set(text);
		originalComments.connect(originalDocument);
		newCompareItem.setOriginalComments(originalComments);
		newCompareItem.setOriginalDocument(originalDocument);

		//Editable comments are a copy of the original comments but associated with the document that is presented in the UI
		AnnotationModel editableComments = new CommentAnnotationManager();
		newCompareItem.set(text);
		editableComments.connect(newCompareItem);
		newCompareItem.setEditableComments(editableComments);

		if (comments == null) {
			return;
		}

		sortComments(comments);

		for (CommentInfo commentInfo : comments) {
			//We only consider the comments that apply to the revision
			if (commentInfo.getSide() != null && !commentInfo.getSide().equals("REVISION")) { //$NON-NLS-1$
				continue;
			}
			IRegion lineInfo;
			try {
				int insertionLineInDocument = 0;
				int insertionPosition = 0;
				String lineDelimiter = ""; //$NON-NLS-1$
				if (commentInfo.getLine() > 0) {
					insertionLineInDocument = commentInfo.getLine() - 1;
					lineInfo = originalDocument.getLineInformation(insertionLineInDocument);
					lineDelimiter = originalDocument.getLineDelimiter(insertionLineInDocument);
					insertionPosition = lineInfo.getOffset() + lineInfo.getLength()
							+ (lineDelimiter == null ? 0 : lineDelimiter.length());
				}
				int commentTextIndex = insertionPosition;
				String formattedComment = CommentPrettyPrinter.printComment(commentInfo);
				int commentTextLength = formattedComment.length();
				if (lineDelimiter == null) {
					formattedComment = originalDocument.getDefaultLineDelimiter() + formattedComment;
					commentTextIndex += originalDocument.getDefaultLineDelimiter().length();
				}
				formattedComment += originalDocument.getDefaultLineDelimiter();
				originalDocument.replace(insertionPosition, 0, formattedComment);
				newCompareItem.replace(insertionPosition, 0, formattedComment);
				originalComments.addAnnotation(new GerritCommentAnnotation(commentInfo, formattedComment),
						new Position(commentTextIndex, commentTextLength));
				editableComments.addAnnotation(new GerritCommentAnnotation(commentInfo, formattedComment),
						new Position(commentTextIndex, commentTextLength));
			} catch (BadLocationException e) {
				logger.debug("Exception merging text and comments.", e); //$NON-NLS-1$
			}
		}
	}

	private static void sortComments(ArrayList<CommentInfo> comments) {
		Collections.sort(comments, new Comparator<CommentInfo>() {
			@Override
			public int compare(CommentInfo o1, CommentInfo o2) {
				if (o1.getLine() == o2.getLine()) {
					return o1.getUpdated().compareTo(o2.getUpdated());
				}
				if (o1.getLine() < o2.getLine()) {
					return -1;
				}
				return 1;
			}
		});
		Collections.reverse(comments);
	}
}
