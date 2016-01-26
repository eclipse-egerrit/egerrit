/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.egerrit.core.command.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.egerrit.core.command.ChangeOption;
import org.eclipse.egerrit.core.command.QueryChangesCommand;
import org.eclipse.egerrit.core.exception.EGerritException;
import org.eclipse.egerrit.internal.model.ChangeInfo;
import org.junit.Test;

/**
 * Test suite for {@link org.eclipse.egerrit.core.command.QueryChangesCommand}
 *
 * @since 1.0
 */
public class QueryChangesCommandTest extends CommandTestWithSimpleReview {
	/**
	 * Test method for {@link org.eclipse.egerrit.core.command.GerritCommand#call()}.
	 */
	@Test
	public void testCall() {
		QueryChangesCommand command = fGerrit.queryChanges();
		ChangeInfo[] result = null;
		try {
			result = command.call();
		} catch (EGerritException e) {
			fail(e.getMessage());
		}

		List<ChangeInfo> listChangeInfo = Arrays.asList(result);
		boolean isFound = false;

		for (ListIterator<ChangeInfo> iter = listChangeInfo.listIterator(); iter.hasNext();) {
			ChangeInfo element = iter.next();
			if (element.getId().equals(change_id)) {
				isFound = true;
				break;

			}
		}
		assertTrue(isFound);
	}

	@Test
	public void testLimitCounts() {
		QueryChangesCommand command = fGerrit.queryChanges();
		command.setMaxNumberOfResults(2);
		ChangeInfo[] result = null;
		try {
			result = command.call();
			assertEquals(2, result.length);
		} catch (EGerritException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testRestartFrom() {
		int numberOfChanges = 0;
		List<ChangeInfo> results = new ArrayList<>();
		{
			//Setup
			QueryChangesCommand command = fGerrit.queryChanges();
			command.setMaxNumberOfResults(2);
			try {
				results = new ArrayList(Arrays.asList(command.call()));
				numberOfChanges = results.size();
			} catch (EGerritException e) {
				fail(e.getMessage());
			}
		}

		if (numberOfChanges >= 2) {
			try {
				{
					QueryChangesCommand command = fGerrit.queryChanges();
					command.setSkipNumberOfResults(0);
					command.setMaxNumberOfResults(1);
					ChangeInfo[] toRemove = command.call();
					results.remove(toRemove[0]);
				}
				{
					QueryChangesCommand command2 = fGerrit.queryChanges();
					command2.setSkipNumberOfResults(1);
					command2.setMaxNumberOfResults(1);
					ChangeInfo[] toRemove2 = command2.call();
					results.remove(toRemove2[0]);
				}
				assertEquals(0, results.size());
			} catch (EGerritException e) {
				fail();
			}
		}
	}

	@Test
	public void testCallWithOption() {
		QueryChangesCommand command = fGerrit.queryChanges();
		command.addOption(ChangeOption.DETAILED_LABELS);
		command.addOption(ChangeOption.ALL_REVISIONS);
		command.addOption(ChangeOption.ALL_FILES);
		command.addQuery(change_id);
		ChangeInfo[] result = null;
		try {
			result = command.call();
			assertNotNull(result[0].getRevisions().get(commit_id));
		} catch (EGerritException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testWithMultipleQueries() {
		QueryChangesCommand command = fGerrit.queryChanges();
		command.addQuery("is:open");
		command.addQuery("owner:self");
		ChangeInfo[] result = null;
		try {
			result = command.call();
			assertTrue(result.length > 0);
		} catch (EGerritException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testByMessage() {
		QueryChangesCommand command = fGerrit.queryChanges();
		command.addQuery("message:\"Test commit message\""); //$NON-NLS-1$
		ChangeInfo[] result = null;
		try {
			result = command.call();
			assertTrue(result.length > 0);
			assertEquals("Test commit message", result[0].getSubject()); //$NON-NLS-1$
		} catch (EGerritException e) {
			fail(e.getMessage());
		}
	}
}
