package com.kixeye.chassis.transport.util;

/*
 * #%L
 * Java Transport API
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.StringWriter;
import java.util.Random;

import com.google.common.base.Charsets;

/**
 * Random string utils.
 * 
 * @author ebahtijaragic
 */
public final class RandomStringUtils {
	private RandomStringUtils() {}
	
	private static final char[] LOWER_LETTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final char[] CAPITAL_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	private static final char[] NUMBERS = "0123456789".toCharArray();
	
	private static final int LOWER_LETTERS_LENTH = LOWER_LETTERS.length;
	private static final int CAPITAL_LETTERS_LENTH = CAPITAL_LETTERS.length;
	private static final int LETTERS_LENGTH = LOWER_LETTERS_LENTH + CAPITAL_LETTERS_LENTH;
	private static final int NUMBERS_LENGTH = NUMBERS.length;
	
	private static final int ALPHANUMERIC_CHARS_LENGTH = LETTERS_LENGTH + NUMBERS_LENGTH;
	
	/**
	 * Generates a random string.
	 * 
	 * @param size
	 * @param random
	 * @return
	 */
	public static String randomString(int size, Random random) {
		byte[] val = new byte[size];
		
		random.nextBytes(val);
		
		return new String(val, Charsets.US_ASCII);
	}
	
	/**
	 * Generates a random alphanumeric string.
	 * 
	 * @param size
	 * @param random
	 * @return
	 */
	public static String randomAlphaNumericString(int size, Random random) {
		final StringWriter sw = new StringWriter(size);
		
		for (int i = 0; i < size; i++) {
			char charValue;
			int randomValue = random.nextInt(ALPHANUMERIC_CHARS_LENGTH);
			
			if (randomValue >= LOWER_LETTERS_LENTH) {
				if (randomValue >= LETTERS_LENGTH) {
					charValue = NUMBERS[randomValue - LETTERS_LENGTH];
				} else {
					charValue = CAPITAL_LETTERS[randomValue - LOWER_LETTERS_LENTH];
				}
			} else {
				charValue = LOWER_LETTERS[randomValue];
			}
			
			sw.append(charValue);
		}
		
		return sw.toString();
	}
}
