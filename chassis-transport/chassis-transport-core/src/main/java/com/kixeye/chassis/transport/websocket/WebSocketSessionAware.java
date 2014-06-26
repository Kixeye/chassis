package com.kixeye.chassis.transport.websocket;

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
