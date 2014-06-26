package com.kixeye.chassis.transport;

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

import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.RandomStringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.hibernate.validator.constraints.Length;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.kixeye.chassis.transport.crypto.SymmetricKeyCryptoUtils;
import com.kixeye.chassis.transport.dto.Envelope;
import com.kixeye.chassis.transport.dto.Header;
import com.kixeye.chassis.transport.dto.ServiceError;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.serde.converter.JsonMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.ProtobufMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.XmlMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.YamlMessageSerDe;
import com.kixeye.chassis.transport.utils.SocketUtils;
import com.kixeye.chassis.transport.websocket.ActionMapping;
import com.kixeye.chassis.transport.websocket.ActionPayload;
import com.kixeye.chassis.transport.websocket.QueuingWebSocketListener;
import com.kixeye.chassis.transport.websocket.WebSocketController;
import com.kixeye.chassis.transport.websocket.WebSocketMessageRegistry;
import com.kixeye.chassis.transport.websocket.WebSocketPskFrameProcessor;

/**
 * Tests the WebSocket transport.
 * 
 * @author ebahtijaragic
 */
public class WebSocketTransportTest {
	@Test
	public void testEmptyWebSocketFrameUsingBinary() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestWebSocketService.class);

		WebSocketClient wsClient = new WebSocketClient();
		
		try {
			//start server
			context.refresh();

			// start client
			wsClient.start();

			final MessageSerDe serDe = context.getBean(JsonMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			QueuingWebSocketListener listener = new QueuingWebSocketListener(serDe, messageRegistry, null);
			
			WebSocketSession session = (WebSocketSession)wsClient.connect(listener, new URI("ws://localhost:" +  properties.get("websocket.port") + "/" + serDe.getMessageFormatName()))
					.get(5000, TimeUnit.MILLISECONDS);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(new byte[0]));
			
			ServiceError error = listener.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(error);
			Assert.assertEquals("EMPTY_ENVELOPE", error.code);
			Assert.assertEquals("STOPPED", session.getState());
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
			}
		}
	}
	
	@Test
	public void testEmptyWebSocketFrameUsingText() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestWebSocketService.class);

		WebSocketClient wsClient = new WebSocketClient();
		
		try {
			//start server
			context.refresh();

			// start client
			wsClient.start();

			final MessageSerDe serDe = context.getBean(JsonMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			QueuingWebSocketListener listener = new QueuingWebSocketListener(serDe, messageRegistry, null);
			
			WebSocketSession session = (WebSocketSession)wsClient.connect(listener, new URI("ws://localhost:" +  properties.get("websocket.port") + "/" + serDe.getMessageFormatName()))
					.get(5000, TimeUnit.MILLISECONDS);
			
			session.getRemote().sendString("");
			
			ServiceError error = listener.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(error);
			Assert.assertEquals("EMPTY_ENVELOPE", error.code);
			Assert.assertEquals("STOPPED", session.getState());
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
			}
		}
	}
	
	@Test
	public void testWebSocketServiceWithJson() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestWebSocketService.class);

		WebSocketClient wsClient = new WebSocketClient();
		
		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(JsonMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			messageRegistry.registerType("stuff", TestObject.class);
			
			wsClient.start();

			QueuingWebSocketListener webSocket = new QueuingWebSocketListener(serDe, messageRegistry, null);

			Session session = wsClient.connect(webSocket, new URI("ws://localhost:" +  properties.get("websocket.port") + "/" + serDe.getMessageFormatName())).get(5000, TimeUnit.MILLISECONDS);

			Envelope envelope = new Envelope("getStuff", null, null, Lists.newArrayList(new Header("testheadername", Lists.newArrayList("testheaderval"))), null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			TestObject response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			byte[] rawStuff = serDe.serialize(new TestObject("more stuff"));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			envelope = new Envelope("getStuff", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			rawStuff = serDe.serialize(new TestObject(RandomStringUtils.randomAlphanumeric(100)));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			ServiceError error = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.code);
			
			envelope = new Envelope("expectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.code, error.code);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.description, error.description);

			envelope = new Envelope("unexpectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.code);
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
			}
		}
	}
	
	@Test
	public void testWebSocketServiceWithJsonWithPskEncryption() throws Exception {
	    // create AES shared key cipher
        Security.addProvider(new BouncyCastleProvider());
	    KeyGenerator kgen = KeyGenerator.getInstance("AES", "BC");
	    kgen.init(128);
	    SecretKey key = kgen.generateKey();
	    byte[] aesKey = key.getEncoded();

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");

		properties.put("websocket.crypto.enabled", "true");
		properties.put("websocket.crypto.cipherProvider", "BC");
		properties.put("websocket.crypto.cipherTransformation", "AES/ECB/PKCS7Padding");
		properties.put("websocket.crypto.secretKeyAlgorithm", "AES");
		properties.put("websocket.crypto.secretKeyData", BaseEncoding.base16().encode(aesKey));
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestWebSocketService.class);
		
		WebSocketClient wsClient = new WebSocketClient();
		
		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(JsonMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			messageRegistry.registerType("stuff", TestObject.class);
			
			wsClient.start();

			QueuingWebSocketListener webSocket = new QueuingWebSocketListener(serDe, messageRegistry, context.getBean(WebSocketPskFrameProcessor.class));

			Session session = wsClient.connect(webSocket, new URI("ws://localhost:" +  properties.get("websocket.port") + "/" + serDe.getMessageFormatName())).get(5000, TimeUnit.MILLISECONDS);

			Envelope envelope = new Envelope("getStuff", null, null, Lists.newArrayList(new Header("testheadername", Lists.newArrayList("testheaderval"))), null);
			
			byte[] rawEnvelope = serDe.serialize(envelope);
			rawEnvelope = SymmetricKeyCryptoUtils.encrypt(rawEnvelope, 0, rawEnvelope.length, key, "AES/ECB/PKCS7Padding", "BC");
			
			session.getRemote().sendBytes(ByteBuffer.wrap(rawEnvelope));
			
			TestObject response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			byte[] rawStuff = serDe.serialize(new TestObject("more stuff"));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));

			rawEnvelope = serDe.serialize(envelope);
			rawEnvelope = SymmetricKeyCryptoUtils.encrypt(rawEnvelope, 0, rawEnvelope.length, key, "AES/ECB/PKCS7Padding", "BC");
			
			session.getRemote().sendBytes(ByteBuffer.wrap(rawEnvelope));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			envelope = new Envelope("getStuff", null, null, null);
			
			rawEnvelope = serDe.serialize(envelope);
			rawEnvelope = SymmetricKeyCryptoUtils.encrypt(rawEnvelope, 0, rawEnvelope.length, key, "AES/ECB/PKCS7Padding", "BC");
			
			session.getRemote().sendBytes(ByteBuffer.wrap(rawEnvelope));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			rawStuff = serDe.serialize(new TestObject(RandomStringUtils.randomAlphanumeric(100)));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			rawEnvelope = serDe.serialize(envelope);
			rawEnvelope = SymmetricKeyCryptoUtils.encrypt(rawEnvelope, 0, rawEnvelope.length, key, "AES/ECB/PKCS7Padding", "BC");
			
			session.getRemote().sendBytes(ByteBuffer.wrap(rawEnvelope));
			
			ServiceError error = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.code);
			
			envelope = new Envelope("expectedError", null, null, null);
			
			rawEnvelope = serDe.serialize(envelope);
			rawEnvelope = SymmetricKeyCryptoUtils.encrypt(rawEnvelope, 0, rawEnvelope.length, key, "AES/ECB/PKCS7Padding", "BC");
			
			session.getRemote().sendBytes(ByteBuffer.wrap(rawEnvelope));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.code, error.code);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.description, error.description);

			envelope = new Envelope("unexpectedError", null, null, null);
			
			rawEnvelope = serDe.serialize(envelope);
			rawEnvelope = SymmetricKeyCryptoUtils.encrypt(rawEnvelope, 0, rawEnvelope.length, key, "AES/ECB/PKCS7Padding", "BC");
			
			session.getRemote().sendBytes(ByteBuffer.wrap(rawEnvelope));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.code);
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
			}
		}
	}

	@Test
	public void testWebSocketServiceWithJsonWithWss() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("secureWebsocket.enabled", "true");
		properties.put("secureWebsocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("secureWebsocket.hostname", "localhost");
		properties.put("secureWebsocket.selfSigned", "true");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestWebSocketService.class);

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);

		WebSocketClient wsClient = new WebSocketClient(sslContextFactory);
		
		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(JsonMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			messageRegistry.registerType("stuff", TestObject.class);
			
			wsClient.start();

			QueuingWebSocketListener webSocket = new QueuingWebSocketListener(serDe, messageRegistry, null);

			Session session = wsClient.connect(webSocket, new URI("wss://localhost:" +  properties.get("secureWebsocket.port") + "/" + serDe.getMessageFormatName())).get(5000, TimeUnit.MILLISECONDS);

			Envelope envelope = new Envelope("getStuff", null, null, Lists.newArrayList(new Header("testheadername", Lists.newArrayList("testheaderval"))), null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			TestObject response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			byte[] rawStuff = serDe.serialize(new TestObject("more stuff"));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			envelope = new Envelope("getStuff", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			rawStuff = serDe.serialize(new TestObject(RandomStringUtils.randomAlphanumeric(100)));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			ServiceError error = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.code);
			
			envelope = new Envelope("expectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.code, error.code);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.description, error.description);

			envelope = new Envelope("unexpectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.code);
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
			}
		}
	}

	@Test
	public void testWebSocketServiceWithProtobuf() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestWebSocketService.class);

		WebSocketClient wsClient = new WebSocketClient();
		
		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(ProtobufMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			messageRegistry.registerType("stuff", TestObject.class);
			
			wsClient.start();
			
			QueuingWebSocketListener webSocket = new QueuingWebSocketListener(serDe, messageRegistry, null);

			Session session = wsClient.connect(webSocket, new URI("ws://localhost:" +  properties.get("websocket.port") + "/" + serDe.getMessageFormatName())).get(5000, TimeUnit.MILLISECONDS);

			Envelope envelope = new Envelope("getStuff", null, null, Lists.newArrayList(new Header("testheadername", Lists.newArrayList("testheaderval"))), null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			TestObject response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			byte[] rawStuff = serDe.serialize(new TestObject("more stuff"));

			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			envelope = new Envelope("getStuff", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			rawStuff = serDe.serialize(new TestObject(RandomStringUtils.randomAlphanumeric(100)));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			ServiceError error = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.code);
			
			envelope = new Envelope("expectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.code, error.code);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.description, error.description);

			envelope = new Envelope("unexpectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.code);
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
				while (context.isActive()) {
					Thread.sleep(100);
				}
			}
		}
	}
	
	@Test
	public void testWebSocketServiceWithXml() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestWebSocketService.class);

		WebSocketClient wsClient = new WebSocketClient();
		
		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(XmlMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			messageRegistry.registerType("stuff", TestObject.class);
			
			wsClient.start();
			
			QueuingWebSocketListener webSocket = new QueuingWebSocketListener(serDe, messageRegistry, null);

			Session session = wsClient.connect(webSocket, new URI("ws://localhost:" +  properties.get("websocket.port") + "/" + serDe.getMessageFormatName())).get(5000, TimeUnit.MILLISECONDS);

			Envelope envelope = new Envelope("getStuff", null, null, Lists.newArrayList(new Header("testheadername", Lists.newArrayList("testheaderval"))), null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			TestObject response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			byte[] rawStuff = serDe.serialize(new TestObject("more stuff"));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5000, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			envelope = new Envelope("getStuff", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			rawStuff = serDe.serialize(new TestObject(RandomStringUtils.randomAlphanumeric(100)));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			ServiceError error = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.code);
			
			envelope = new Envelope("expectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.code, error.code);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.description, error.description);

			envelope = new Envelope("unexpectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.code);
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
				while (context.isActive()) {
					Thread.sleep(100);
				}
			}
		}
	}
	
	@Test
	public void testWebSocketServiceWithYaml() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestWebSocketService.class);

		WebSocketClient wsClient = new WebSocketClient();
		
		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(YamlMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			messageRegistry.registerType("stuff", TestObject.class);
			
			wsClient.start();
			
			QueuingWebSocketListener webSocket = new QueuingWebSocketListener(serDe, messageRegistry, null);

			Session session = wsClient.connect(webSocket, new URI("ws://localhost:" +  properties.get("websocket.port") + "/" + serDe.getMessageFormatName())).get(5000, TimeUnit.MILLISECONDS);

			Envelope envelope = new Envelope("getStuff", null, null, Lists.newArrayList(new Header("testheadername", Lists.newArrayList("testheaderval"))), null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			TestObject response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			byte[] rawStuff = serDe.serialize(new TestObject("more stuff"));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			envelope = new Envelope("getStuff", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			rawStuff = serDe.serialize(new TestObject(RandomStringUtils.randomAlphanumeric(100)));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			ServiceError error = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.code);
			
			envelope = new Envelope("expectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.code, error.code);
			Assert.assertEquals(TestWebSocketService.EXPECTED_EXCEPTION.description, error.description);

			envelope = new Envelope("unexpectedError", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			error = webSocket.getResponse(5, TimeUnit.SECONDS);

			Assert.assertNotNull(error);
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.code);
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
				while (context.isActive()) {
					Thread.sleep(100);
				}
			}
		}
	}
	
	@WebSocketController
	public static class TestWebSocketService {
		public static final ServiceError EXPECTED_EXCEPTION = new ServiceError("expected", "Expected exception!");
		
		public final AtomicReference<TestObject> stuff = new AtomicReference<>(new TestObject("stuff"));

		@ActionMapping("expectedError")
		public void throwExpectedException() {
			throw new ServiceException(EXPECTED_EXCEPTION);
		}

		@ActionMapping("unexpectedError")
		public void throwUnexpectedException() {
			throw new RuntimeException("unexpected");
		}
		
		@ActionMapping("getStuff")
		public DeferredResult<TestObject> getStuff() {
			DeferredResult<TestObject> result = new DeferredResult<TestObject>();
			result.setResult(stuff.get());
			return result;
		}

		@ActionMapping("setStuff")
		public ListenableFuture<TestObject> setStuff(@ActionPayload @Valid TestObject request) {
            SettableFuture<TestObject> future = SettableFuture.create();
            future.set(stuff.get());
            stuff.set(request);
            return future;
		}
	}
	
	public static class TestObject {
		@NotNull
		@Length(max=50)
		public String value = null;
		
		public TestObject(String value) {
			this.value = value;
		}
		
		public TestObject() {
			
		}
	}
}
