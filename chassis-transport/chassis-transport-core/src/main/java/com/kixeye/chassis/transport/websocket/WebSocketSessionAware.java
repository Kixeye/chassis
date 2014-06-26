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

/**
 * An interfaces for handlers that want to be aware of websocket sessions.
 * 
 * @author ebahtijaragic
 */
public interface WebSocketSessionAware {
	/**
	 * Gets invoked on a new websocket session is created and this handler is recognized by that websocket session.
	 * 
	 * @param session
	 */
	public void onWebSocketSessionCreated(WebSocketSession session);
	
	/**
	 * Gets invoked when a websocket session gets removed.
	 * 
	 * @param session
	 */
	public void onWebSocketSessionRemoved(WebSocketSession session);
}
