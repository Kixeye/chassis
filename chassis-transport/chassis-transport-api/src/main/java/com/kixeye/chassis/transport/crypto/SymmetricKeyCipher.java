package com.kixeye.chassis.transport.crypto;

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

import java.security.GeneralSecurityException;
import java.security.Key;

/**
 * A cipher for processing symmetric keys.
 * 
 * @author ebahtijaragic
 */
public class SymmetricKeyCipher {
	private final String cipherProvider;
	private final String cipherTransformation;

	private final Key secretKey;
	
	/**
	 * A cipher for processing symmetric key encryption.
	 * 
	 * @param cipherProvider
	 * @param cipherTransformation
	 * @param secretKeyAlgorithm
	 * @param secretKeyData
	 * @throws Exception
	 */
	public SymmetricKeyCipher(
			String cipherProvider,
			String cipherTransformation, 
			String secretKeyAlgorithm, 
			byte[] secretKeyBlob) throws Exception {
		this.cipherProvider = cipherProvider;
		this.cipherTransformation = cipherTransformation;
		this.secretKey = SymmetricKeyCryptoUtils.loadRawSecretKey(secretKeyBlob, secretKeyAlgorithm);
	}
	
	/**
	 * Encrypts the given data.
	 * 
	 * @param data
	 * @param offset
	 * @param length
	 * @return
	 * @throws GeneralSecurityException
	 */
	public byte[] encrypt(byte[] data, int offset, int length) throws GeneralSecurityException {
		return SymmetricKeyCryptoUtils.encrypt(data, offset, length, secretKey, cipherTransformation, cipherProvider);
	}
	
	/**
	 * Decrypts the given data.
	 * 
	 * @param data
	 * @param offset
	 * @param length
	 * @return
	 * @throws GeneralSecurityException
	 */
	public byte[] decrypt(byte[] data, int offset, int length) throws GeneralSecurityException {
		return SymmetricKeyCryptoUtils.decrypt(data, offset, length, secretKey, cipherTransformation, cipherProvider);
	}
}
