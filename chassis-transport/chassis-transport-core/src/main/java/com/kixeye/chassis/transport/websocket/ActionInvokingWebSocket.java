package com.kixeye.chassis.transport.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.kixeye.chassis.transport.ExceptionServiceErrorMapper;
import com.kixeye.chassis.transport.dto.Envelope;
import com.kixeye.chassis.transport.dto.ServiceError;
import com.kixeye.chassis.transport.serde.MessageSerDe;

/**
 * Listens to websocket messages and forwards it to the correct bean.
 * 
 * @author ebahtijaragic
 */
public class ActionInvokingWebSocket implements WebSocketListener {
	private static final Logger logger = LoggerFactory.getLogger(ActionInvokingWebSocket.class);
	
	@Autowired
	private WebSocketMessageMappingRegistry mappingRegistry;
	
	@Autowired
	private WebSocketMessageRegistry messageRegistry;
	
	@Autowired
	private DefaultListableBeanFactory beanFactory;
	
	@Autowired
	private Validator messageValidator;

	@Autowired
    private WebSocketPskFrameProcessor pskFrameProcessor;
	
	private Session session;
	
	private WebSocketSession webSocketSession = new WebSocketSession(this);
	
	private ServletUpgradeRequest upgradeRequest;

	private ServletUpgradeResponse upgradeResponse;
	
	private MessageSerDe serDe;
	
	private ListeningExecutorService serviceExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
	private ExecutorService responseExecutor = Executors.newSingleThreadExecutor();
	
	private LoadingCache<String, Object> handlerCache = CacheBuilder.newBuilder()
			.removalListener(new RemovalListener<String, Object>() {
				public void onRemoval(RemovalNotification<String, Object> notification) {
					Class<?> handlerClass = null;
					
					try {
						handlerClass = Class.forName(notification.getKey());
					} catch (ClassNotFoundException e) {
						logger.error("Unexpected exception", e);
					}
					
					if (handlerClass != null) {
						String[] beanNames = beanFactory.getBeanNamesForType(handlerClass);

						if (beanNames != null && beanNames.length > 0) {
							if (beanFactory.isPrototype(beanNames[0])) {
								if (notification.getValue() instanceof WebSocketSessionAware) {
									WebSocketSessionAware webSocketSessionAwareHandler = (WebSocketSessionAware)notification.getValue();
									
									webSocketSessionAwareHandler.onWebSocketSessionRemoved(webSocketSession);
								}
								
								beanFactory.destroyBean(notification.getValue());
							} // else this is a singleton and we don't do anything with singletons
						} // this shouldn't happen
					} // this shouldn't happen either
				}
			})
			.build(new CacheLoader<String, Object>() {
				public Object load(String handlerClassName) throws Exception {
					Class<?> handlerClass = Class.forName(handlerClassName);
					
					String[] beanNames = beanFactory.getBeanNamesForType(handlerClass);
					
					if (beanNames != null && beanNames.length > 0) {
						Object handler = beanFactory.getBean(beanNames[0]);
						
						if (handler instanceof WebSocketSessionAware) {
							WebSocketSessionAware webSocketSessionAwareHandler = (WebSocketSessionAware)handler;
							
							webSocketSessionAwareHandler.onWebSocketSessionCreated(webSocketSession);
						}
						
						return handler;
					} else {
						throw new RuntimeException("No beans exist for handler: " + handlerClass);
					}
				}
			});
	
	public void onWebSocketBinary(byte[] payload, int offset, int length) {
		try {
			// don't accept empty frames
			if (payload == null || length < 1) {
				throw new WebSocketServiceException(new ServiceError("EMPTY_ENVELOPE", "Empty envelope!"), "UNKNOWN", null);
			}

			// check if we need to do psk encryption
			byte[] processedPayload = pskFrameProcessor.processIncoming(payload, offset, length);
			
			if (processedPayload != payload) {
				payload = processedPayload;
				offset = 0;
				length = payload.length;
			}
			
			// get the envelope
			final WebSocketEnvelope envelope = new WebSocketEnvelope(serDe.deserialize(payload, offset, length, Envelope.class));
			
			// gets all the actions
			Collection<WebSocketAction> actions = mappingRegistry.getActionMethods(envelope.getAction());
			
			final AtomicInteger invokedActions = new AtomicInteger(0);
			
			// invokes them
			for (final WebSocketAction action : actions) {
				// get and validate type ID
				Class<?> messageClass = null;
				
				if (StringUtils.isNotBlank(envelope.getTypeId())) {
					messageClass = messageRegistry.getClassByTypeId(envelope.getTypeId());
				}
				
				// validate if action has a payload class that it needs
				if (action.getPayloadClass() != null && messageClass == null) {
					throw new WebSocketServiceException(new ServiceError("INVALID_TYPE_ID", "Unknown type ID!"), envelope.getAction(), envelope.getTransactionId());
				}
				
				// invoke this action if allowed
				if (action.canInvoke(webSocketSession, messageClass)) {
                    invokedActions.incrementAndGet();
                    
					final Object handler = handlerCache.get(action.getHandlerClass().getName());
					final Class<?> finalMessageClass = messageClass;
					
					ListenableFuture<DeferredResult<?>> invocation = serviceExecutor.submit(new Callable<DeferredResult<?>>() {
						@Override
						public DeferredResult<?> call() throws Exception {

							// then invoke
							return action.invoke(handler, new RawWebSocketMessage<>(envelope.getPayload(), finalMessageClass, messageValidator, serDe), envelope, webSocketSession);
						}

                        private String getTokenValue(WebSocketEnvelope envelope, String headerName) {
                            return Iterables.get(envelope.getHeaders().get(headerName), 0, null);
                        }
                    });
					
					Futures.addCallback(invocation, new FutureCallback<DeferredResult<?>>() {
						public void onSuccess(DeferredResult<?> result) {
							if (result != null) {
								result.setResultHandler(new DeferredResultHandler() {
									@Override
									public void handleResult(Object result) {
										if (result instanceof Exception) {
											onFailure((Exception)result);
											return;
										}
										
										sendResponse(result);
									}
								});
							}
						}

						public void onFailure(Throwable t) {
							if (t instanceof InvocationTargetException) {
								t = ((InvocationTargetException)t).getTargetException();
							}
							
							ServiceError error = ExceptionServiceErrorMapper.mapException(t);

							if (error != null && !ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE.equals(error.code)) {
								logger.error("Unexpected exception throw while executing action [{}]", envelope.getAction(), t);
							}
							
							sendResponse(error);
						}
						
						public Future<Void> sendResponse(Object response) {
							try {
								return sendMessage(envelope.getAction(), envelope.getTransactionId(), response);
							} catch (IOException | GeneralSecurityException e) {
								logger.error("Unable to send message to channel", e);
								
								return Futures.immediateFuture(null);
							}
						}
						
					}, responseExecutor);
				}
			}
			
			// make sure we actually invoked something
			if (invokedActions.get() < 1) {
				throw new WebSocketServiceException(new ServiceError("INVALID_ACTION_MAPPING", "No actions invoked."), envelope.getAction(), envelope.getTransactionId());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    public void onWebSocketConnect(Session session) {
		logger.info(this.toString() + " - Session connected [{}].", session.toString());
		this.session = session;
	}

	public void onWebSocketError(Throwable cause) {
		logger.error("Unexpected socket error", cause);
		
		if (session.isOpen()) {
			try {
				String action = null;
				String txId = null;
				
				WebSocketServiceException serviceException = null;
				Throwable currentCause = cause;
				
				while (currentCause != null) {
					if (currentCause instanceof WebSocketServiceException) {
						serviceException = (WebSocketServiceException)currentCause;
					}
					
					currentCause = currentCause.getCause();
				}
				
				if (serviceException != null) {
					action = serviceException.action;
					txId = serviceException.transactionId;
				} else {
					action = "UNKNOWN";
				}
				
				sendMessage(action, txId, ExceptionServiceErrorMapper.mapException(serviceException == null ? cause : serviceException)).get();
			} catch (Exception e) {
				logger.error("Unexpected error", e);
			}
			
			session.close();
		}
	}

	public void onWebSocketText(String message) {
		byte[] data = message.getBytes(Charsets.UTF_8);
		
		onWebSocketBinary(data, 0, data.length);
	}
	
	public synchronized void onWebSocketClose(int statusCode, String reason) {
		logger.info(this.toString() + " - Session disconnected [{}]. Reason: [{}]", session.toString(), reason);
		
		try {
			handlerCache.invalidateAll();
		} finally {
			beanFactory.destroyBean(this);
		}
	}
	
	/**
	 * Gets the websocket session.
	 * 
	 * @return
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	protected Future<Void> sendMessage(String action, String transactionId, Object obj) throws IOException, GeneralSecurityException {
		String typeId = messageRegistry.getTypeIdByClass(obj.getClass());

		if (typeId == null) {
			throw new RuntimeException("Unable to determine type ID for class: " +  obj.getClass());
		}
		
		byte[] payload = serDe.serialize(obj);
		
		return sendMessage(action, transactionId, typeId, ByteBuffer.wrap(payload));
	}
	
	/**
	 * Gets the websocket session.
	 * 
	 * @return
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	protected Future<Void> sendMessage(String action, String transactionId, String typeId, ByteBuffer payload) throws IOException, GeneralSecurityException {
		Envelope envelope = new Envelope(action, typeId, transactionId, payload);
		
		// generate blob
		byte[] envelopeBlob = serDe.serialize(envelope);

		// check if we need to do psk encryption
		envelopeBlob = pskFrameProcessor.processOutgoing(envelopeBlob, 0, envelopeBlob.length);
		
		return session.getRemote().sendBytesByFuture(ByteBuffer.wrap(envelopeBlob));
	}
	
	/**
	 * Gets the websocket session.
	 * 
	 * @return
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	protected Future<Void> sendContent(InputStream inputStream) throws IOException, GeneralSecurityException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		IOUtils.copyLarge(inputStream, baos);

		// generate blob
		byte[] contentBlob = serDe.serialize(baos.toByteArray());

		// check if we need to do psk encryption
		contentBlob = pskFrameProcessor.processOutgoing(contentBlob, 0, contentBlob.length);
		
		return session.getRemote().sendBytesByFuture(ByteBuffer.wrap(contentBlob));
	}

	/**
	 * @return the serDe
	 */
	public MessageSerDe getSerDe() {
		return serDe;
	}

	/**
	 * @param serDe the serDe to set
	 */
	public void setSerDe(MessageSerDe serDe) {
		this.serDe = serDe;
	}
	
	/**
	 * @return the upgradeRequest
	 */
	public ServletUpgradeRequest getUpgradeRequest() {
		return upgradeRequest;
	}

	/**
	 * @param upgradeRequest the upgradeRequest to set
	 */
	public void setUpgradeRequest(ServletUpgradeRequest upgradeRequest) {
		this.upgradeRequest = upgradeRequest;
	}

	/**
	 * @return the upgradeResponse
	 */
	public ServletUpgradeResponse getUpgradeResponse() {
		return upgradeResponse;
	}

	/**
	 * @param upgradeResponse the upgradeResponse to set
	 */
	public void setUpgradeResponse(ServletUpgradeResponse upgradeResponse) {
		this.upgradeResponse = upgradeResponse;
	}

	/**
	 * Returns true if we're connected.
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return session != null && session.isOpen();
	}
}