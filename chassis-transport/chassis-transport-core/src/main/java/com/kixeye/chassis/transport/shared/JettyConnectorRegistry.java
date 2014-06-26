package com.kixeye.chassis.transport.shared;

/*
 * #%L
 * Chassis Transport Core
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

import com.google.common.base.Charsets;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.core.io.DefaultResourceLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;

/**
 * Registers connectors with Jetty.
 * 
 * @author ebahtijaragic
 */
@SuppressWarnings("deprecation")
public class JettyConnectorRegistry {
    /**
     * Register to listen to HTTP.
     * 
     * @param server
     * @param address
     */
    public static void registerHttpConnector(Server server, InetSocketAddress address) {
    	ServerConnector connector = new ServerConnector(server);
        connector.setHost(address.getHostName());
        connector.setPort(address.getPort());
    	
        server.addConnector(connector);
    }
    
    /**
     * Register to listen to HTTPS.
     * 
     * @param server
     * @param address
     * @throws Exception 
     */
    public static void registerHttpsConnector(Server server, InetSocketAddress address, boolean selfSigned,
    		boolean mutualSsl, String keyStorePath, String keyStoreData, String keyStorePassword, String keyManagerPassword,
    		String trustStorePath, String trustStoreData, String trustStorePassword, String[] excludedCipherSuites) throws Exception {
    	// SSL Context Factory
        SslContextFactory sslContextFactory = new SslContextFactory();

        if (selfSigned) {
        	char[] passwordChars = UUID.randomUUID().toString().toCharArray();
        	
        	KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        	keyStore.load(null, passwordChars);
        	
        	KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");  
        	keyPairGenerator.initialize(1024);  
        	KeyPair keyPair = keyPairGenerator.generateKeyPair();
        	
        	X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
        	
        	v3CertGen.setSerialNumber(BigInteger.valueOf(new SecureRandom().nextInt()).abs());
            v3CertGen.setIssuerDN(new X509Principal("CN=" + "kixeye.com" + ", OU=None, O=None L=None, C=None"));
            v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30));
            v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365*10)));
            v3CertGen.setSubjectDN(new X509Principal("CN=" + "kixeye.com" + ", OU=None, O=None L=None, C=None"));
            
            v3CertGen.setPublicKey(keyPair.getPublic());  
            v3CertGen.setSignatureAlgorithm("MD5WithRSAEncryption");  
        	
            X509Certificate privateKeyCertificate = v3CertGen.generateX509Certificate(keyPair.getPrivate());
            
            keyStore.setKeyEntry("selfSigned", keyPair.getPrivate(), passwordChars,  
                    new java.security.cert.Certificate[]{ privateKeyCertificate });  

        	ByteArrayOutputStream keyStoreBaos = new ByteArrayOutputStream();
        	keyStore.store(keyStoreBaos, passwordChars);
        	
        	keyStoreData = new String(Hex.encode(keyStoreBaos.toByteArray()), Charsets.UTF_8);
        	keyStorePassword = new String(passwordChars);
        	keyManagerPassword = keyStorePassword;
        	
        	sslContextFactory.setTrustAll(true);
        }

    	KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        if(StringUtils.isNotBlank(keyStoreData)){
            keyStore.load(new ByteArrayInputStream(Hex.decode(keyStoreData)), keyStorePassword.toCharArray());
        } else if (StringUtils.isNotBlank(keyStorePath)) {
            try (InputStream inputStream = new DefaultResourceLoader().getResource(keyStorePath).getInputStream()) {
                keyStore.load(inputStream, keyStorePassword.toCharArray());
            }
        }

        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        if(StringUtils.isBlank(keyManagerPassword)){
            keyManagerPassword = keyStorePassword;
        }
        sslContextFactory.setKeyManagerPassword(keyManagerPassword);
        KeyStore trustStore = null;
        if (StringUtils.isNotBlank(trustStoreData)) {
        	trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        	trustStore.load(new ByteArrayInputStream(Hex.decode(trustStoreData)), trustStorePassword.toCharArray());
        } else if (StringUtils.isNotBlank(trustStorePath)) {
        	trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        	try (InputStream inputStream = new DefaultResourceLoader().getResource(trustStorePath).getInputStream()) {
        		trustStore.load(inputStream, trustStorePassword.toCharArray());
        	}
        }
        if (trustStore != null) {
	        sslContextFactory.setTrustStore(trustStore);
	        sslContextFactory.setTrustStorePassword(trustStorePassword);
        }
    	sslContextFactory.setNeedClientAuth(mutualSsl);
        sslContextFactory.setExcludeCipherSuites(excludedCipherSuites);

        // SSL Connector
    	ServerConnector connector = new ServerConnector(server, 
    			new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()),
    			new HttpConnectionFactory()
    	);
        connector.setHost(address.getHostName());
        connector.setPort(address.getPort());
    	
        server.addConnector(connector);
    }
}
