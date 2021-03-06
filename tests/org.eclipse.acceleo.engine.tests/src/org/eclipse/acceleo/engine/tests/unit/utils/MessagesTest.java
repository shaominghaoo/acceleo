/*******************************************************************************
 * Copyright (c) 2006, 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.acceleo.engine.tests.unit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.acceleo.engine.AcceleoEngineMessages;
import org.junit.Test;

/**
 * Tests Messages class. These tests' successful completion heavily depends on
 * org.eclipse.acceleo.engine.acceleoenginemessages.properties file contents.
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 */
@SuppressWarnings({"unchecked", "nls", })
public class MessagesTest {
	/** Expected result of the parameterisable keys (only used if locale is en). */
	private final String[] expectedForParameterisable = {"Operation {0}({1}) is undefined on type {2}.",
			"Could not create lost file for {0}. Lost content : {1}",
			"Could not find public template {0} in module {1}.", };

	/** Contains the expected results for the valid keys (only used if locale is en). */
	private final String[] expectedForValidKeys = {"Start of user code", "Could not dispose open buffers.",
			"Cannot invoke non-public module element for generation.",
			"Arguments of a generation cannot be null.", };

	/** These will be used when testing message retrieval with invalid keys. */
	private final String[] invalidKeys = {"invalidKey", "AcceleoEvaluationContext.CleanUpError1", "",
			"\u00ec", };

	/** Contains possible parameters for the messages. */
	private final Object[] messageParameters = {null, "", "Foehn", -1, new Long(10), '\u0043',
			new HashSet<Object>(), "0x6c9a.^\\/", };

	/**
	 * These two are valid, parameterisable keys. See
	 * org.eclipse.acceleo.engine.acceleoenginemessages.properties.
	 */
	private final String[] parameterisableKeys = {"AcceleoEvaluationEnvironment.UndefinedOperation",
			"AcceleoEvaluationContext.LostContent", "AcceleoService.UndefinedTemplate", };

	/**
	 * These are valid, un-parameterisable keys. See
	 * org.eclipse.acceleo.engine.acceleoenginemessages.properties.
	 */
	private final String[] validKeys = {"usercode.start", "AcceleoEvaluationContext.CleanUpError",
			"AcceleoEngine.IllegalInvocation", "AcceleoService.NullArguments", };

	/**
	 * Tests {@link AcceleoEngineMessages#getString(String, Object...)} with an invalid key. Expects the
	 * String
	 * 
	 * <pre>
	 * &quot;!&quot; + key + &quot;!&quot;
	 * </pre>
	 * 
	 * to be returned. Parameters won't affect result here.
	 */
	@Test
	public void testFormattedGetStringInvalidKey() {
		for (int i = 0; i < messageParameters.length; i++) {
			for (int j = i; j < messageParameters.length; j++) {
				final Object[] parameters = partialArrayCopy(messageParameters, i, j);
				for (String invalidKey : invalidKeys) {
					assertEquals("Unexpected result of getString() with an invalid key.",
							'!' + invalidKey + '!', AcceleoEngineMessages.getString(invalidKey, parameters));
				}
			}
		}
	}

	/**
	 * Tests {@link AcceleoEngineMessages#getString(String, Object...)} with <code>null</code> key. Expects a
	 * NullPointerException to be thrown. Parameters won't affect result here.
	 */
	@Test
	public void testFormattedGetStringNullKey() {
		for (int i = 0; i < messageParameters.length; i++) {
			for (int j = i; j < messageParameters.length; j++) {
				final Object[] parameters = partialArrayCopy(messageParameters, i, j);
				try {
					AcceleoEngineMessages.getString(null, parameters);
					fail("Calling getString() with null key did not throw NullPointerException.");
				} catch (NullPointerException e) {
					// This was expected
				}
			}
		}
	}

	/**
	 * Tests {@link AcceleoEngineMessages#getString(String, Object...)} with valid keys.
	 * <p>
	 * If the System locale is configured for english language, expects the String associated to the key in
	 * the properties file to be returned with all occurences of <code>&quot;{[0-9]*}&quot;</code> replaced by
	 * the correct parameter if any. Otherwise, expects the key to have been found, and the parameters to be
	 * correctly substituted.
	 * </p>
	 */
	@Test
	public void testFormattedGetStringValidKey() {
		for (int i = 0; i < messageParameters.length; i++) {
			for (int j = i; j < messageParameters.length; j++) {
				final Object[] parameters = partialArrayCopy(messageParameters, i, j);
				for (int k = 0; k < parameterisableKeys.length; k++) {
					String expectedResult = expectedForParameterisable[k];
					int parameterCount = 0;
					while (expectedResult.matches(".*\\{[0-9]+\\}.*") && parameterCount < parameters.length) {
						if (parameters[parameterCount] == null) {
							expectedResult = expectedResult.replaceFirst("\\{[0-9]+\\}", "null");
						} else {
							expectedResult = expectedResult.replaceFirst("\\{[0-9]+\\}",
									parameters[parameterCount].toString());
						}
						parameterCount++;
					}
					Locale previousLocale = null;
					if (Locale.getDefault() != Locale.ENGLISH) {
						previousLocale = Locale.getDefault();
					}
					Locale.setDefault(Locale.ENGLISH);
					assertEquals("Unexpected formatted String returned by getString(String, Object...).",
							expectedResult, AcceleoEngineMessages.getString(parameterisableKeys[k],
									parameters));
					if (previousLocale != null) {
						Locale.setDefault(previousLocale);
					} else {
						Locale.setDefault(Locale.FRENCH);
					}
					final String result = AcceleoEngineMessages.getString(parameterisableKeys[k], parameters);
					assertFalse("Message class did not find an existing parameterisable key.", result
							.equals('!' + parameterisableKeys[k] + '!'));
					for (int l = 0; l < parameterCount; l++) {
						if (parameters[l] != null) {
							assertTrue("Message class did not substitute the parameter in the result.",
									result.contains(parameters[l].toString()));
						} else {
							assertTrue("Message class did not substitute the parameter in the result.",
									result.contains("null"));
						}
					}
					if (previousLocale == null) {
						Locale.setDefault(Locale.ENGLISH);
					}
				}
			}
		}
	}

	/**
	 * Tests {@link AcceleoEngineMessages#getString(String, Object...)} with valid keys and <code>null</code>
	 * as formatting parameter. Expects the result to be the same as the
	 * {@link AcceleoEngineMessages#getString(String)}.
	 */
	@Test
	public void testFormattedGetStringValidKeyNullParameter() {
		for (int i = 0; i < parameterisableKeys.length; i++) {
			assertEquals("Unexpected formatted String returned by getString(String, Object...).",
					AcceleoEngineMessages.getString(parameterisableKeys[i]), AcceleoEngineMessages.getString(
							parameterisableKeys[i], (Object[])null));
		}
	}

	/**
	 * Tests {@link AcceleoEngineMessages#getString(String)} with an invalid key. Expects the String
	 * 
	 * <pre>
	 * &quot;!&quot; + key + &quot;!&quot;
	 * </pre>
	 * 
	 * to be returned.
	 */
	@Test
	public void testUnFormattedGetStringInvalidKey() {
		for (String invalidKey : invalidKeys) {
			assertEquals("Unexpected result of getString() with an invalid key.", '!' + invalidKey + '!',
					AcceleoEngineMessages.getString(invalidKey));
		}
	}

	/**
	 * Tests {@link AcceleoEngineMessages#getString(String)} with <code>null</code> argument. Expects a
	 * NullPointerException to be thrown.
	 */
	@Test
	public void testUnFormattedGetStringNullKey() {
		try {
			AcceleoEngineMessages.getString(null);
			fail("Calling getString() with null argument did not throw NullPointerException.");
		} catch (NullPointerException e) {
			// This was expected
		}
	}

	/**
	 * Tests {@link AcceleoEngineMessages#getString(String)} with valid keys. Expects the String associated to
	 * the key in the properties file to be returned.
	 */
	@Test
	public void testUnFormattedGetStringValidKey() {
		for (int i = 0; i < validKeys.length; i++) {
			Locale previousLocale = null;
			if (Locale.getDefault() != Locale.ENGLISH) {
				previousLocale = Locale.getDefault();
			}
			Locale.setDefault(Locale.ENGLISH);
			String result = AcceleoEngineMessages.getString(validKeys[i]);
			assertEquals("Unexpected String returned by getString(String).", expectedForValidKeys[i], result);
			if (previousLocale != null) {
				Locale.setDefault(previousLocale);
			} else {
				Locale.setDefault(Locale.FRENCH);
			}
			assertFalse("Message class did not find an existing parameterisable key.", result
					.equals('!' + validKeys[i] + '!'));
			if (previousLocale == null) {
				Locale.setDefault(Locale.ENGLISH);
			}
		}
	}

	/**
	 * This will return a partial copy of an array.
	 * 
	 * @param <T>
	 *            Type of the copied array.
	 * @param original
	 *            Array to be copied.
	 * @param start
	 *            starting index of the copy.
	 * @param end
	 *            end index of the copy.
	 * @return Array containing a copy of the given range from <code>original</code>.
	 */
	private <T> T[] partialArrayCopy(T[] original, int start, int end) {
		final int range = end - start;
		if (range < 0) {
			throw new IllegalArgumentException("Illegal copy range.");
		}
		final T[] copy = (T[])Array.newInstance(original.getClass().getComponentType(), range);
		System.arraycopy(original, start, copy, 0, Math.min(original.length - start, range));
		return copy;
	}
}
