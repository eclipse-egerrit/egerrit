/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Francois Chouinard - Initial API and implementation
 *******************************************************************************/
package org.eclipse.egerrit.ui.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//------------------------------------------------------------------------
//Test suite
//------------------------------------------------------------------------

@RunWith(Suite.class)
@SuiteClasses({ EGerritUIPluginTest.class, OpenCompareEditorTests.class, CommentExtractorTest.class,
		ChangeIdExtractorTest.class, EnterHttpInSearchTest.class })
//------------------------------------------------------------------------
//Constructor
//------------------------------------------------------------------------
public class AllGerritUITests {
}
