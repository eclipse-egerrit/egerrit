/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson - initial API and implementation
 *******************************************************************************/

package org.eclipse.egerrit.internal.core.rest;

/**
 * Data model object for
 * <a href="https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#submit-input">SubmitInput</a>
 */
public class SubmitInput {

	// Whether the request should wait for the merge to complete.
	// If false the request returns immediately after the change has been added to the merge queue and the caller can’t
	// know whether the change could be merged successfully.
	// The SubmitInput entity contains information for submitting a change.
	private boolean wait_for_merge;

	/**
	 * @return the wait_for_merge
	 */
	public boolean isWait_for_merge() {
		return wait_for_merge;
	}

	/**
	 * @param wait_for_merge
	 *            the wait_for_merge to set
	 */
	public void setWait_for_merge(boolean wait_for_merge) {
		this.wait_for_merge = wait_for_merge;
	}
}
