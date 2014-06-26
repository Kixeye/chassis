package com.kixeye.chassis.transport.websocket;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kixeye.chassis.transport.dto.Envelope;
import com.kixeye.chassis.transport.dto.ServiceError;
import com.kixeye.chassis.transport.serde.MessageSerDe;

/**
 * A web-socket listener that can be used in tests.
 * 
 * @author ebahtijaragic
 */
public class QueuingWebSocketListener implements WebSocketListener {
	private static final Logger logger = LoggerFactory.getLogger(QueuingWebSocketListener.class);
	
	private final MessageSerDe serDe;
	private final WebSocketMessageRegistry messageRegistry;
	private final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
	private final WebSocketPskFrameProcessor pskFrameProcessor;
	
	public QueuingWebSocketListener(MessageSerDe serDe, WebSocketMessageRegistry messageRegistry, WebSocketPskFrameProcessor pskFrameProcessor) {
		this.serDe = serDe;
		this.messageRegistry = messageRegistry;
		this.pskFrameProcessor = pskFrameProcessor;
	}

	public void onWebSocketBinary(byte[] payload, int offset, int length) {
		try {
			if (pskFrameProcessor != null) {
				byte[] processedPayload = pskFrameProcessor.processIncoming(payload, offset, length);
				
				if (processedPayload != payload) {
					payload = processedPayload;
					offset = 0;
					length = processedPayload.length;
				}
			}
			
			final Envelope envelope = serDe.deserialize(payload, offset, length, Envelope.class);
			
			Object message = null;
			
			if (StringUtils.isNotBlank(envelope.typeId)) {
				Class<?> messageClass = messageRegistry.getClassByTypeId(envelope.typeId);
				
				if (messageClass == null) {
					throw new RuntimeException("Unknown type id: " + envelope.typeId);
				}
				
				if (envelope.payload != null) {
					byte[] rawPayload = envelope.payload.array();
					message = serDe.deserialize(rawPayload, 0, rawPayload.length, messageClass);
				}
			}
			
			queue.offer(message);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void onWebSocketClose(int statusCode, String reason) { }

	public void onWebSocketConnect(Session session) { }

	public void onWebSocketError(Throwable cause) { }

	public void onWebSocketText(String message) { }
	
	@SuppressWarnings("unchecked")
	public <T> T getResponse(long timeout, TimeUnit unit) throws Exception {
		Object response = queue.poll(timeout, unit);
		
		if (response instanceof ServiceError) {
			logger.warn("Got error: [{}]", new ObjectMapper().writeValueAsString(response));
		}
		
		return (T)response;
	}
}
