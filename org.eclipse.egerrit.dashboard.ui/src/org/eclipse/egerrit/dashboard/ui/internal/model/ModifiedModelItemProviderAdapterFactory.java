/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Ericsson - Initial Implementation of the view sorter
 ******************************************************************************/
package org.eclipse.egerrit.dashboard.ui.internal.model;

import org.eclipse.egerrit.internal.model.provider.ModelItemProviderAdapterFactory;
import org.eclipse.emf.common.notify.Adapter;

/**
 * Factory returning our modified providers
 */
public class ModifiedModelItemProviderAdapterFactory extends ModelItemProviderAdapterFactory {

	@Override
	public Adapter createChangeInfoAdapter() {
		if (changeInfoItemProvider == null) {
			changeInfoItemProvider = new ReviewTableLabelProvider(this);
		}

		return changeInfoItemProvider;
	}

}
