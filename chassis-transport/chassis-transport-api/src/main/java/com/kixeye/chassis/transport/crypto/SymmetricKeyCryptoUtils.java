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
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Does symmetric key encrypt/decrypt.
 * 
 * @author ebahtijaragic
 */
public class SymmetricKeyCryptoUtils {
	static {
        Security.addProvider(new BouncyCastleProvider());
	}
	
	/**
	 * Loads a raw secret key.
	 * 
	 * @param key
	 * @param keyAlgorithm
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static Key loadRawSecretKey(byte[] key, String keyAlgorithm) throws GeneralSecurityException {
		return new SecretKeySpec(key, keyAlgorithm);
	}
	
	/**
	 * Loads a cipher.
	 * 
	 * @param transformation
	 * @param provider
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static Cipher loadCipher(String transformation, String provider) throws GeneralSecurityException {
		if (StringUtils.isNotBlank(provider)) {
			return Cipher.getInstance(transformation, provider);
		} else {
			return Cipher.getInstance(transformation);
		}
	}
	
	/**
	 * Encrypts a data blob using the given key and cipher.
	 * 
	 * @param data
	 * @param offset
	 * @param length
	 * @param key
	 * @param cipher
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static byte[] encrypt(byte[] data, int offset, int length, Key key, String cipherTransformation, String cipherProvider) throws GeneralSecurityException {
		Cipher cipher = loadCipher(cipherTransformation, cipherProvider);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		
		return cipher.doFinal(data, offset, length);
	}
	
	/**
	 * Decrypts a data blob using the given key and cipher.
	 * 
	 * @param data
	 * @param offset
	 * @param length
	 * @param key
	 * @param cipher
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static byte[] decrypt(byte[] data, int offset, int length, Key key, String cipherTransformation, String cipherProvider) throws GeneralSecurityException {
		Cipher cipher = loadCipher(cipherTransformation, cipherProvider);
		cipher.init(Cipher.DECRYPT_MODE, key);
		
		return cipher.doFinal(data, offset, length);
	}
}
