package com.kixeye.chassis.transport.websocket;

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
