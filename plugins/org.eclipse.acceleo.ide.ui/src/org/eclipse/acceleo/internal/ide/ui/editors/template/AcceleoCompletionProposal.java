/*******************************************************************************
 * Copyright (c) 2008, 2011 Obeo and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.acceleo.internal.ide.ui.editors.template;

import com.google.common.base.Throwables;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Implementation of a completion proposal for Acceleo.
 * <p>
 * Base implementation is a verbatim copy of jface's CompletionProposal since it has for some reason been made
 * final. All we are really interested in here is the implementation of ICompletionProposalExtension2.
 * </p>
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 */
public class AcceleoCompletionProposal implements ICompletionProposal, ICompletionProposalExtension2 {
	/** The string to be displayed in the completion proposal popup. */
	private String fDisplayString;

	/** The replacement string. */
	private String fReplacementString;

	/** The replacement offset. */
	private int fReplacementOffset;

	/** The replacement length. */
	private int fReplacementLength;

	/** The cursor position after this proposal has been applied. */
	private int fCursorPosition;

	/** The image to be displayed in the completion proposal popup. */
	private Image fImage;

	/** The context information of this proposal. */
	private IContextInformation fContextInformation;

	/** The additional info of this proposal. */
	private String fAdditionalProposalInfo;

	/**
	 * Creates a new completion proposal based on the provided information. The replacement string is
	 * considered being the display string too. All remaining fields are set to <code>null</code>.
	 * 
	 * @param replacementString
	 *            the actual string to be inserted into the document
	 * @param replacementOffset
	 *            the offset of the text to be replaced
	 * @param replacementLength
	 *            the length of the text to be replaced
	 * @param cursorPosition
	 *            the position of the cursor following the insert relative to replacementOffset
	 */
	public AcceleoCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
			int cursorPosition) {
		this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null);
	}

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided information.
	 * 
	 * @param replacementString
	 *            the actual string to be inserted into the document
	 * @param replacementOffset
	 *            the offset of the text to be replaced
	 * @param replacementLength
	 *            the length of the text to be replaced
	 * @param cursorPosition
	 *            the position of the cursor following the insert relative to replacementOffset
	 * @param image
	 *            the image to display for this proposal
	 * @param displayString
	 *            the string to be displayed for the proposal
	 * @param contextInformation
	 *            the context information associated with this proposal
	 * @param additionalProposalInfo
	 *            the additional information associated with this proposal
	 */
	/*
	 * CHECKSTYLE:OFF : CompletionProposals need a lot of parameters...
	 */
	public AcceleoCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
			int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
			String additionalProposalInfo) {
		// CHECKSTYLE:ON
		Assert.isNotNull(replacementString);
		Assert.isTrue(replacementOffset >= 0);
		Assert.isTrue(replacementLength >= 0);
		Assert.isTrue(cursorPosition >= 0);

		fReplacementString = replacementString;
		fReplacementOffset = replacementOffset;
		fReplacementLength = replacementLength;
		fCursorPosition = cursorPosition;
		fImage = image;
		fDisplayString = displayString;
		fContextInformation = contextInformation;
		fAdditionalProposalInfo = additionalProposalInfo;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(IDocument document) {
		try {
			document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
		} catch (BadLocationException x) {
			// ignore
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	public Point getSelection(IDocument document) {
		return new Point(fReplacementOffset + fCursorPosition, 0);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return fContextInformation;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return fImage;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		if (fDisplayString != null) {
			return fDisplayString;
		}
		return fReplacementString;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return fAdditionalProposalInfo;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer,
	 *      char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		final IDocument doc = viewer.getDocument();

		try {
			// Replace the whole string, this will avoid case-sensitive issues.
			doc.replace(fReplacementOffset, fReplacementLength, fReplacementString);
		} catch (BadLocationException e) {
			Throwables.propagate(e);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer,
	 *      boolean)
	 */
	public void selected(ITextViewer viewer, boolean smartToggle) {
		// Don't react to selection/unselection
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface.text.ITextViewer)
	 */
	public void unselected(ITextViewer viewer) {
		// Don't react to selection/unselection

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument,
	 *      int, org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		boolean valid = false;
		if (offset >= fReplacementOffset && offset < fReplacementOffset + fReplacementString.length()) {
			try {
				final String start = document.get(fReplacementOffset, offset - fReplacementOffset);
				valid = start.equalsIgnoreCase(fReplacementString.substring(0, offset - fReplacementOffset));
			} catch (BadLocationException x) {
				// return false
			}
		}
		if (valid && event != null) {
			// adapt replacement range to document change
			int delta = 0;
			if (event.fText != null) {
				delta = event.fText.length();
			}
			delta -= event.fLength;
			fReplacementLength = Math.max(fReplacementLength + delta, 0);
		}
		return valid;
	}

	/**
	 * Offers a getter for the replacement string.
	 * 
	 * @return The replacement string.
	 */
	public String getReplacementString() {
		return fReplacementString;
	}

	/**
	 * Offers a getter for the replacement length.
	 * 
	 * @return The replacement length.
	 */
	public int getReplacementLength() {
		return fReplacementLength;
	}
}
