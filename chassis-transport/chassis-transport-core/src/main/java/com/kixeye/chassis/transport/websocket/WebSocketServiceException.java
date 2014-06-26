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

import com.kixeye.chassis.transport.ServiceException;
import com.kixeye.chassis.transport.dto.ServiceError;

/**
 * A websocket service exception.
 * 
 * @author ebahtijaragic
 */
public class WebSocketServiceException  extends ServiceException {
	private static final long serialVersionUID = -4838549665424544519L;
	
	public final String action;
	public final String transactionId;
	
	/**
	 * An WebSocket service exception.
	 */
	public WebSocketServiceException(ServiceError error, String action, String transactionId) {
		super(error);
		
		this.action = action;
		this.transactionId = transactionId;
	}
	
	/**
	 * An WebSocket service exception.
	 */
	public WebSocketServiceException(ServiceError error, String action, String transactionId, Throwable cause) {
		super(error, cause);

		this.action = action;
		this.transactionId = transactionId;
	}
}
