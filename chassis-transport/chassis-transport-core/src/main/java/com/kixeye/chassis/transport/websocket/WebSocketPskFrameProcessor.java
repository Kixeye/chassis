package com.kixeye.chassis.transport.websocket;

import java.security.GeneralSecurityException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.kixeye.chassis.transport.crypto.SymmetricKeyCipher;

/**
 * Processes a websocket frame with PSK encryption if encryption is enabled.
 * 
 * @author ebahtijaragic
 */
@Component
public class WebSocketPskFrameProcessor {
	private static final DefaultResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();
	
	private final SymmetricKeyCipher cipher;
	
	/**
	 * @param cipherProvider
	 * @param cipherTransformation
	 * @param secretKeyProvider
	 * @param secretKeyAlgorithm
	 * @param secretKeyData
	 * @param secretKeyPath
	 * @param enabled
	 * @param secretKey
	 */
	@Autowired
	public WebSocketPskFrameProcessor(
			@Value("${websocket.crypto.cipherProvider:}") String cipherProvider,
			@Value("${websocket.crypto.cipherTransformation:}") String cipherTransformation, 
			@Value("${websocket.crypto.secretKeyAlgorithm:}") String secretKeyAlgorithm, 
			@Value("${websocket.crypto.secretKeyData:}") String secretKeyData,
			@Value("${websocket.crypto.secretKeyPath:}") String secretKeyPath, 
			@Value("${websocket.crypto.enabled:false}") boolean enabled) throws Exception {
		if (enabled) {
			byte[] secretKey = null;
			
			if (StringUtils.isNoneBlank(secretKeyData)) {
				secretKey = BaseEncoding.base16().decode(secretKeyData);
			} else if (StringUtils.isNoneBlank(secretKeyPath)) {
				Resource secretKeyFile = RESOURCE_LOADER.getResource(secretKeyPath);
				
				secretKey = IOUtils.toByteArray(secretKeyFile.getInputStream());
			} else {
				throw new IllegalArgumentException("Neither secret key data nor path were provided.");
			}
			
			cipher = new SymmetricKeyCipher(cipherProvider, cipherTransformation, secretKeyAlgorithm, secretKey);
		} else {
			cipher = null;
		}
	}
	
	/**
	 * Processes an incoming frame.
	 * 
	 * @param payload
	 * @return
	 * @throws GeneralSecurityException
	 */
	public byte[] processIncoming(byte[] payload, int offset, int length) throws GeneralSecurityException {
		if (cipher != null) {
			return cipher.decrypt(payload, offset, length);
		} else {
			return payload;
		}
	}
	
	/**
	 * Processes an outgoing frame.
	 * 
	 * @param payload
	 * @return
	 * @throws GeneralSecurityException
	 */
	public byte[] processOutgoing(byte[] payload, int offset, int length) throws GeneralSecurityException {
		if (cipher != null) {
			return cipher.encrypt(payload, offset, length);
		} else {
			return payload;
		}
	}
}
