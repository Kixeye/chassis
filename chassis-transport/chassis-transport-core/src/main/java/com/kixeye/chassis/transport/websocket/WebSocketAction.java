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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.validation.Valid;

import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.async.DeferredResult;

import com.google.common.collect.Lists;
import com.kixeye.chassis.transport.websocket.responseconverter.DeferredResultResponseConverter;
import com.kixeye.chassis.transport.websocket.responseconverter.ListenableFutureResponseConverter;
import com.kixeye.chassis.transport.websocket.responseconverter.ObservableResponseConverter;
import com.kixeye.chassis.transport.websocket.responseconverter.WebSocketResponseConverter;

/**
 * A web-socket action.
 * 
 * @author ebahtijaragic
 */
public class WebSocketAction {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketAction.class);

    static private final AtomicReference<List<WebSocketResponseConverter>> responseConverters;

	private final Method method;
	
	private Class<?> payloadClass;
	private int payloadParameterIndex;
	private Class<?> responseClass;
	
	private boolean validatePayload = false;
	private boolean takesEnvelope = false;
	
	private Map<String, String> requirements;
	
	private Map<WebSocketAction.ParameterType, Integer> parameterTypes = new HashMap<>();

	private enum ParameterType {
		ACTION_PAYLOAD,
		TRANSACTION_ID,
		ENVELOPE,
		WEB_SOCKET_SESSION,
        CUSTOM
	}

    static {
        List<WebSocketResponseConverter> tmp = new ArrayList<>();
        tmp.add( new DeferredResultResponseConverter() );
        tmp.add( new ListenableFutureResponseConverter() );
        tmp.add( new ObservableResponseConverter() );
        responseConverters = new AtomicReference<>(Collections.unmodifiableList(tmp));
    }
	
	public WebSocketAction(Method method, Map<String, String> requirements) {
		this.method = method;
		this.requirements = requirements;
		
		scanMethod();
	}

    static public void addWebSocketResponseConverter(WebSocketResponseConverter converter) {
        synchronized (responseConverters) {
            List<WebSocketResponseConverter> newConverters = Lists.newArrayList(responseConverters.get() );
            newConverters.add(0,converter);
            responseConverters.set( Collections.unmodifiableList(newConverters) );
        }
    }
    
	/**
	 * Returns true if this action can be safely invoked using the given class.
	 * 
	 * @param session
     * @param messageClass
	 * @return
	 */
	public boolean canInvoke(WebSocketSession session, Class<?> messageClass) {
		if (takesEnvelope) {
			// if we take envelopes in this action, always pass
			return doesMeetRequirements(session);
		} else {
			if (messageClass == null) {
				if (payloadClass == null) {
					return doesMeetRequirements(session);
				} else {
					return false;
				}
			} else {
				return ObjectUtils.equals(payloadClass, messageClass);
			}
		}
	}
	
	/**
	 * Returns true if the current session meets the action requirements.
	 * 
	 * @param session
	 * @return
	 */
	private boolean doesMeetRequirements(WebSocketSession session) {
		if (requirements == null) {
			return true;
		}
		
		for (Entry<String, String> requirement : requirements.entrySet()) {
			if (!session.containsProperty(requirement.getKey(), requirement.getValue())) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Scans the method and caches parameter info.
	 */
	private void scanMethod() {
		Class<?>[] parameters = method.getParameterTypes();

		for (int p = 0; p < parameters.length; p++) {
			// check if we want a web-socket session
			if (WebSocketSession.class.equals(parameters[p])) {
				if (parameterTypes.containsKey(ParameterType.WEB_SOCKET_SESSION)) {
					throw new RuntimeException("Cannot have multiple parameters as web-socket sessions on method: " + method);
				}
				
				parameterTypes.put(ParameterType.WEB_SOCKET_SESSION, p);
			} else if (WebSocketEnvelope.class.equals(parameters[p])) {
				if (parameterTypes.containsKey(ParameterType.ENVELOPE)) {
					throw new RuntimeException("Cannot have multiple parameters as envelope on method: " + method);
				}
				
				takesEnvelope = true;
				
				parameterTypes.put(ParameterType.ENVELOPE, p);
			} else {
				for (Annotation annotation : method.getParameterAnnotations()[p]) {
					if (annotation instanceof ActionPayload) {
						if (parameterTypes.containsKey(ParameterType.ACTION_PAYLOAD)) {
							throw new RuntimeException("Cannot have multiple parameters marked as payloads on method: " + method);
						}
						
						parameterTypes.put(ParameterType.ACTION_PAYLOAD, p);
						payloadClass = parameters[p];
						payloadParameterIndex = p;
						
						for (Annotation secondaryAnnotation : method.getParameterAnnotations()[p]) {
							if (secondaryAnnotation instanceof Valid) {
								validatePayload = true;
								break;
							}
						}
						break;
					} else if (annotation instanceof ActionTransactionId) {
						if (parameterTypes.containsKey(ParameterType.TRANSACTION_ID)) {
							throw new RuntimeException("Cannot have multiple parameters marked as payloads on method: " + method);
						}
						
						parameterTypes.put(ParameterType.TRANSACTION_ID, p);
						break;
					}
				}
			}
		}
		
		responseClass = method.getReturnType();
		
		if (responseClass.equals(Void.TYPE)) {
			responseClass = null;
		} else if (responseClass.equals(DeferredResult.class)) {
			responseClass = (Class<?>)((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
		}
	}
	
	/**
	 * Invokes this action.
	 * 
	 * @param handler
	 * @param message
     * @param envelope
	 * @param session
	 * @return
	 * @throws MethodArgumentNotValidException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public DeferredResult<?> invoke(Object handler, RawWebSocketMessage<?> message, WebSocketEnvelope envelope, WebSocketSession session) throws Exception {
		Object response = null;
		
		// invoke the method
		Object[] parameters = new Object[method.getParameterTypes().length];

		Integer payloadParamIndex = parameterTypes.get(ParameterType.ACTION_PAYLOAD);
		if (payloadParamIndex != null) {
			parameters[payloadParamIndex] = message.deserialize(this);
		}
		
		Integer sessionParamIndex = parameterTypes.get(ParameterType.WEB_SOCKET_SESSION);
		if (sessionParamIndex != null) {
			parameters[sessionParamIndex] = session;
		}
		
		Integer transactionParamIndex = parameterTypes.get(ParameterType.TRANSACTION_ID);
		if (transactionParamIndex != null) {
			parameters[transactionParamIndex] = envelope.getTransactionId();
		}
		
		Integer envelopeParamIndex = parameterTypes.get(ParameterType.ENVELOPE);
		if (envelopeParamIndex != null) {
			parameters[envelopeParamIndex] = envelope;
		}

		// now do actual invoke
		response = method.invoke(handler, parameters);
		
		// otherwise there was no error
		if (response != null) {

            // find a converter
            for(WebSocketResponseConverter converter : responseConverters.get() ) {
                if (converter.canConvertResponse(response)) {
                    return converter.convertToDeferredResult(response);
                }
            }

            // default to using the object as is
            final DeferredResult <Object> deferredResult = new DeferredResult<>();
            deferredResult.setResult(response);
            return deferredResult;
		} else {
			return null;
		}
	}

    /**
	 * Gets the method.
	 * 
	 * @return
	 */
	public Method getMethod() {
		return method;
	}
	
	/**
	 * Gets the payload parameter.
	 * 
	 * @return
	 */
	public int getPayloadParameterIndex() {
		return payloadParameterIndex;
	}
	
	/**
	 * Returns true if we should validate the payload.
	 * 
	 * @return
	 */
	public boolean shouldValidatePayload() {
		return validatePayload;
	}
	
	/**
	 * Gets the handler class of this action.
	 * 
	 * @return
	 */
	public Class<?> getHandlerClass() {
		return method.getDeclaringClass();
	}
	
	/**
	 * Gets the payload class.
	 * 
	 * @return
	 */
	public Class<?> getPayloadClass() {
		return payloadClass;
	}
	
	/**
	 * Gets the payload class.
	 * 
	 * @return
	 */
	public Class<?> getResponseClass() {
		return responseClass;
	}
}