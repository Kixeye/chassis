package com.kixeye.chassis.transport.websocket;

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kixeye.chassis.transport.serde.MessageSerDe;


/**
 * Defines a websocket session.
 * 
 * @author ebahtijaragic
 */
public class WebSocketSession {
	private static final Logger logger = LoggerFactory.getLogger(WebSocketSession.class);
	
	private ActionInvokingWebSocket webSocket;
	private ConcurrentHashMap<String, String> properties;
	private Set<Runnable> closeListeners;
	private AtomicBoolean isClosing = new AtomicBoolean(false);
	
	protected WebSocketSession(ActionInvokingWebSocket webSocket) {
		this.webSocket = webSocket;
		this.properties = new ConcurrentHashMap<>();
		this.closeListeners = Collections.newSetFromMap(new ConcurrentHashMap<Runnable, Boolean>());
	}
	
	/**
	 * Adds a close listener.
	 * 
	 * @param listener
	 */
	public void addCloseListener(Runnable listener) {
		closeListeners.add(listener);
	}
	
	/**
	 * Close this 
	 */
	protected void close() {
		isClosing.set(true);
		
		Iterator<Runnable> closeListenersIter = closeListeners.iterator();
		
		while (closeListenersIter.hasNext()) {
			Runnable listener = closeListenersIter.next();
			
			try {
				listener.run();
			} catch (Exception e) {
				logger.error("Unable to notify listener", e);
			}
		}
	}
	
	/**
	 * Returns true if this session is connected.
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return webSocket.isConnected();
	}
	
	/**
	 * Sends a message to the connected web-socket.
	 * 
	 * @param action
	 * @param transactionId
	 * @param message
	 * @throws GeneralSecurityException 
	 */
	public Future<Void> sendMessage(String action, String transactionId, Object message) throws IOException, GeneralSecurityException {
		return webSocket.sendMessage(action, transactionId, message);
	}
	
	/**
	 * Sends the entire content to the websocket.
	 * 
	 * @param inputStream
	 * @throws GeneralSecurityException 
	 */
	public Future<Void> sendContent(InputStream inputStream) throws IOException, GeneralSecurityException {
		return webSocket.sendContent(inputStream);
	}
	
	/**
	 * Sends a message to the connected web-socket.
	 * 
	 * @param action
	 * @param transactionId
	 * @param message
	 * @throws GeneralSecurityException 
	 */
	public Future<Void> sendMessage(String action, String transactionId, String typeId, ByteBuffer payload) throws IOException, GeneralSecurityException {
		return webSocket.sendMessage(action, transactionId, typeId, payload);
	}
	
	/**
	 * Sets a property and returns previous value (if any).
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public Object setProperty(String name, String value) {
		return properties.put(name, value);
	}
	
	/**
	 * Sets a property if one doesn't already exist with the same name.
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public Object setPropertyIfAbsent(String name, String value) {
		return properties.putIfAbsent(name, value);
	}

	/**
	 * Copies the given map to the properties.
	 * 
	 * @param map
	 */
	public void setProperties(Map<String, String> map) {
		properties.putAll(map);
	}
	
	/**
	 * Gets a property, returning null if it doesn't exit.
	 * 
	 * @param name
	 * @return
	 */
	public String getProperty(String name) {
		return properties.get(name);
	}
	
	/**
	 * Removes a property, returning null if it doesn't exist.
	 * 
	 * @param name
	 * @return
	 */
	public String removeProperty(String name) {
		return properties.remove(name);
	}
	
	/**
	 * Returns true if there is a property with the given name.
	 * 
	 * @param name
	 * @return
	 */
	public boolean containsProperty(String name) {
		return properties.contains(name);
	}
	
	/**
	 * Returns true if there is a property with the given name and value.
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public boolean containsProperty(String name, String value) {
		String propValue = properties.get(name);
		
		return ObjectUtils.equals(value, propValue);
	}
	
	/**
	 * Returns true if there is a property with the given name and the value matches using {@link Pattern}.matches().
	 * 
	 * @param name
	 * @param regex
	 * @return
	 */
	public boolean matchesProperty(String name, String regex) {
		String propValue = properties.get(name);
		
		return Pattern.matches(regex, propValue);
	}
	
	/**
	 * Gets the headers for a header name.
	 * 
	 * @param name
	 * @return
	 */
	public List<String> getHeaders(String name) {
		return webSocket.getUpgradeRequest().getHeaders(name);
	}
	
	/**
	 * Gets the header for a header name.
	 * 
	 * @param name
	 * @return
	 */
	public String getHeader(String name) {
		return webSocket.getUpgradeRequest().getHeader(name);
	}
	
	/**
	 * Gets the request path.
	 * 
	 * @return
	 */
	public String getPath() {
		String path = webSocket.getUpgradeRequest().getRequestURI().getPath();
		
		return path.substring(getSerDe().getMessageFormatName().length() + 1, path.length());
	}
	
	/**
	 * Gets the message serde.
	 * 
	 * @return
	 */
	public MessageSerDe getSerDe() {
		return webSocket.getSerDe();
	}
}
