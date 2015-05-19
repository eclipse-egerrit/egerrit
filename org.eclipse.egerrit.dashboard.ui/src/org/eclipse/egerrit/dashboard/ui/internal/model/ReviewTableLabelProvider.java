/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jacques Bouthillier - Initial Implementation of the label provider
 *   Marc-Andre Laperle - Add Topic to dashboard
 *   Marc-Andre Laperle - Add Status to dashboard
 ******************************************************************************/

package org.eclipse.egerrit.dashboard.ui.internal.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.eclipse.egerrit.core.EGerritCorePlugin;
import org.eclipse.egerrit.core.rest.ChangeInfo;
import org.eclipse.egerrit.core.rest.LabelInfo;
import org.eclipse.egerrit.dashboard.ui.GerritUi;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * This class implements the Dashboard-Gerrit UI view label provider.
 *
 * @since 1.0
 */
public class ReviewTableLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider {

	// ------------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------------

	private static final String VERIFIED = "Verified";

	private static final String CODE_REVIEW = "Code-Review"; //$NON-NLS-1$

	private final String EMPTY_STRING = ""; //$NON-NLS-1$

	// +2 Names of images used to represent review-checked
	public static final String CHECKED_IMAGE = "greenCheck.png"; //$NON-NLS-1$

	// -2 Names of images used to represent review-not OK
	public static final String NOT_OK_IMAGE = "redNot.png"; //$NON-NLS-1$

	// -1
	public static final String MINUS_ONE = "minusOne.png"; //$NON-NLS-1$

	// +1
	public static final String PLUS_ONE = "plusOne.png"; //$NON-NLS-1$

	// Names of images used to represent STAR FILLED
	public static final String STAR_FILLED = "starFilled.gif"; //$NON-NLS-1$

	// Names of images used to represent STAR OPEN
	public static final String STAR_OPEN = "starOpen.gif"; //$NON-NLS-1$

	// Value stored to define the state of the review item.
	public static final int NOT_OK_IMAGE_STATE = -2;

	public static final int CHECKED_IMAGE_STATE = 2;

	// Constant for the column with colors: CR, IC and V
	private static Display fDisplay = Display.getCurrent();

	private static Color RED = fDisplay.getSystemColor(SWT.COLOR_RED);

	private static Color GREEN = fDisplay.getSystemColor(SWT.COLOR_DARK_GREEN);

	//Color used depending on the review state
	private static Color DEFAULT_COLOR = fDisplay.getSystemColor(SWT.COLOR_LIST_BACKGROUND);

//	private static Color INCOMING_COLOR = fDisplay.getSystemColor(SWT.COLOR_TITLE_BACKGROUND);
	private static Color INCOMING_COLOR = fDisplay.getSystemColor(SWT.COLOR_WHITE);

	private static Color CLOSED_COLOR = fDisplay.getSystemColor(SWT.COLOR_WHITE);

//	private static Color INCOMING_COLOR = fDisplay.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
//	private static Color CLOSED_COLOR = fDisplay.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

	// For the images
	private static ImageRegistry fImageRegistry = new ImageRegistry();

	/**
	 * Note: An image registry owns all of the image objects registered with it, and automatically disposes of them the
	 * SWT Display is disposed.
	 */
	static {

		String iconPath = "icons/view16/"; //$NON-NLS-1$

		fImageRegistry.put(CHECKED_IMAGE, GerritUi.getImageDescriptor(iconPath + CHECKED_IMAGE));

		fImageRegistry.put(NOT_OK_IMAGE, GerritUi.getImageDescriptor(iconPath + NOT_OK_IMAGE));

		fImageRegistry.put(MINUS_ONE, GerritUi.getImageDescriptor(iconPath + MINUS_ONE));

		fImageRegistry.put(PLUS_ONE, GerritUi.getImageDescriptor(iconPath + PLUS_ONE));

		fImageRegistry.put(STAR_FILLED, GerritUi.getImageDescriptor(iconPath + STAR_FILLED));

		fImageRegistry.put(STAR_OPEN, GerritUi.getImageDescriptor(iconPath + STAR_OPEN));
	}

	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------
	public ReviewTableLabelProvider() {
	}

	// ------------------------------------------------------------------------
	// Methods
	// ------------------------------------------------------------------------

	/**
	 * Return an image representing the state of the object
	 *
	 * @param int aState
	 * @return Image
	 */
	private Image getReviewStateImage(int aState) {
		switch (aState) {
		case 2:
			return fImageRegistry.get(CHECKED_IMAGE);
		case 1:
			return fImageRegistry.get(PLUS_ONE);
		case 0:
			break;
		case -1:
			return fImageRegistry.get(MINUS_ONE);
		case -2:
			return fImageRegistry.get(NOT_OK_IMAGE);
		default:
			break;
		}
		return null;
	}

	/**
	 * Return an image representing the state of the object
	 *
	 * @param int aState
	 * @return Image
	 */
	private Image getVerifyStateImage(int aState) {
		switch (aState) {
		case 2:
		case 1:
			return fImageRegistry.get(CHECKED_IMAGE);
		case 0:
			break;
		case -1:
		case -2:
			return fImageRegistry.get(NOT_OK_IMAGE);
		default:
			break;
		}
		return null;
	}

	/**
	 * Return an image representing the state of the ID object
	 *
	 * @param Boolean
	 *            aState
	 * @return Image
	 */
	private Image getReviewId(Boolean aState) {
		if (aState) {
			// True means the star is filled ??
			return fImageRegistry.get(STAR_FILLED);
		} else {
			//
			return fImageRegistry.get(STAR_OPEN);
		}
	}

	/**
	 * Return the text associated to the column
	 *
	 * @param Object
	 *            structure of the table
	 * @param int column index
	 * @return String text associated to the column
	 */
	@SuppressWarnings("restriction")
	public String getColumnText(Object aObj, int aIndex) {
		// GerritPlugin.Ftracer.traceWarning("getColumnText object: " + aObj
		// + "\tcolumn: " + aIndex);
		if (aObj instanceof ChangeInfo) {
			ChangeInfo reviewSummary = (ChangeInfo) aObj;
//			String value = null;
			switch (aIndex) {
			case 0:
				Boolean starred = reviewSummary.isStarred();
				return starred.toString(); // Needed for the sorter
			case 1:
				return reviewSummary.getChange_id();
			case 2:
				return reviewSummary.getSubject();
			case 3:
				String attribute = reviewSummary.getStatus().toString();
				return attribute;
			case 4:
				return reviewSummary.getOwner().getName();
			case 5:
				return reviewSummary.getProject();
			case 6:
				String branch = reviewSummary.getBranch();
				String topic = reviewSummary.getTopic();
				if (topic != null && !topic.isEmpty()) {
					branch += " (" + reviewSummary.getTopic() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return branch;
			case 7: {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String dateStr = reviewSummary.getUpdated().toString();
				Date dateNew = null;
				try {
					dateNew = format.parse(dateStr);
				} catch (ParseException e) {
					EGerritCorePlugin.logError(e.getMessage());

				}

				return format.format(dateNew).toString();
			}
			case 8: {
				Map<String, LabelInfo> labels = reviewSummary.getLabels();
				if (labels != null && labels.get(CODE_REVIEW) != null) {
					String value = (labels.get(CODE_REVIEW)).getValue();

					if (null != value && !value.equals(EMPTY_STRING)) {
						return formatValue(value);
					}
				}
			}
//			case 8:
//				value = reviewSummary.getAttribute(GerritTask.REVIEW_STATE);
//				if (null != value && !value.equals(EMPTY_STRING)) {
//					return formatValue (value);
//				}
			case 9: {
				Map<String, LabelInfo> labels = reviewSummary.getLabels();
				if (labels != null && labels.get(VERIFIED) != null) {
					String value = (labels.get(VERIFIED)).getValue();

					if (null != value && !value.equals(EMPTY_STRING)) {
						return formatValue(value);
					}
				}

				return EMPTY_STRING;
			}
//			case 9:
//				value = reviewSummary.getAttribute(GerritTask.IS_IPCLEAN);
//				if (null != value && !value.equals(EMPTY_STRING)) {
//					return formatValue (value);
//				}
//                return EMPTY_STRING;
//			case 10:
//				value = reviewSummary.getAttribute(GerritTask.VERIFY_STATE);
//				if (null != value && !value.equals(EMPTY_STRING)) {
//					return formatValue (value);
//				}
//                return EMPTY_STRING;
			default:
				return EMPTY_STRING;
			}
		}
		return EMPTY_STRING;
	}

	/**
	 * Format the numbering value to display
	 *
	 * @param aSt
	 * @return String
	 */
	private String formatValue(String aSt) {
		int val = aSt.equals("") ? 0 : Integer.parseInt(aSt, 10);
		if (val > 0) {
			String st = "+" + aSt;
			return st;
		}
		return aSt;

	}

	/**
	 * Return the image associated to the column
	 *
	 * @param Object
	 *            structure of the table
	 * @param int column index
	 * @return Image Image according to the selected column
	 */
	@SuppressWarnings("restriction")
	public Image getColumnImage(Object aObj, int aIndex) {
		// GerritPlugin.Ftracer
		// .traceWarning("getColumnImage column: " + aIndex);
		Image image = null;
		String value = null;
		if (aObj instanceof ChangeInfo) {
			ChangeInfo reviewSummary = (ChangeInfo) aObj;
			switch (aIndex) {
			case 0:
				Boolean starred = reviewSummary.isStarred();
				value = starred.toString(); // Needed for the sorter
				if (null != value && !value.equals(EMPTY_STRING)) {
					return getReviewId(Boolean.valueOf(value.toLowerCase()));
				}
				break;
			case 1:
				return image;
			case 2:
				return image;
			case 3:
				return image;
			case 4:
				return image;
			case 5:
				return image;
			case 6:
				return image;
			case 7:
//				value = reviewSummary.getAttribute(GerritTask.REVIEW_STATE);
				value = reviewSummary.getStatus().toString();
				if (null != value && !value.equals(EMPTY_STRING)) {
//					int val = Integer.parseInt(value);
//					return getReviewStateImage(val);
				}
				break;
			case 8:
//				value = reviewSummary.getAttribute(GerritTask.IS_IPCLEAN);
//
//				if (null != value && !value.equals(EMPTY_STRING)) {
//					int val = Integer.parseInt(value);
//					return getReviewSate(val);
//				}
//				break;
//			case 9:
//				value = reviewSummary.getAttribute(GerritTask.VERIFY_STATE);

				if (null != value && !value.equals(EMPTY_STRING)) {
					int val = Integer.parseInt(value);
					return getVerifyStateImage(val);
				}
				break;
			default:
				return image;
			}
		}

		return image;
	}

	/**
	 * Adjust the column color
	 *
	 * @param Object
	 *            ReviewTableListItem
	 * @param int columnIndex
	 */
	@Override
	public Color getForeground(Object aElement, int aColumnIndex) {
		if (aElement instanceof ChangeInfo) {
			int value = 0;
			if (aColumnIndex == ReviewTableDefinition.CR.ordinal()
//					|| aColumnIndex == ReviewTableDefinition.IC.ordinal()
					|| aColumnIndex == ReviewTableDefinition.VERIFY.ordinal()) {
				switch (aColumnIndex) {
				case 7: // ReviewTableDefinition.CR.ordinal():
//					st = item.getAttribute(GerritTask.REVIEW_STATE);
//					if (st != null) {
//						value = st.equals(EMPTY_STRING) ? 0 : Integer
//								.parseInt(st);
//					}
					break;
				case 8: // ReviewTableDefinition.IC.ordinal():
//					st = item.getAttribute(GerritTask.IS_IPCLEAN);
//					if (st != null) {
//						value = st.equals(EMPTY_STRING) ? 0 : Integer
//								.parseInt(st);
//					}
//					break;
//				case 9: // ReviewTableDefinition.VERIFY.ordinal():
//					st = item.getAttribute(GerritTask.VERIFY_STATE);
//					if (st != null) {
//						value = st.equals(EMPTY_STRING) ? 0 : Integer
//								.parseInt(st);
//					}
					break;
				}
				if (value < 0) {
					return RED;
				} else if (value > 0) {
					return GREEN;
				}
			}
		}
		return null;
	}

	@Override
	@SuppressWarnings("restriction")
	public Color getBackground(Object aElement, int aColumnIndex) {
		// GerritUi.Ftracer.traceInfo("getBackground column : " +
		// aColumnIndex +
		// " ]: "+ aElement );
		if (aElement instanceof ChangeInfo) {
			ChangeInfo item = (ChangeInfo) aElement;
			//
			// To modify when we can verify the review state
			String state = new Boolean(item.isStarred()).toString();
			if (state != null) {
				if (state.equals(Boolean.toString(true))) {
					return INCOMING_COLOR;

				} else if (state.equals(Boolean.toString(false))) {
					return CLOSED_COLOR;
				}
			}
		}

		return DEFAULT_COLOR;
	}

}
