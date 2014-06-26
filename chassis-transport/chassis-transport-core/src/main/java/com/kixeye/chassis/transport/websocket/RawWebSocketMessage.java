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

import java.nio.ByteBuffer;

import javax.validation.Validator;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.kixeye.chassis.transport.serde.MessageSerDe;

/**
 * A websocket message that contains the raw data and can be deserialized on demand.
 * 
 * @author ebahtijaragic
 */
public class RawWebSocketMessage<T> {
	private ByteBuffer rawData;
	private Class<T> messageClass;
	
	private Validator messageValidator;
	private MessageSerDe serDe;
	
	/**
	 * @param rawData
	 * @param messageClass
	 * @param messageValidator
	 * @param serDe
	 */
	public RawWebSocketMessage(ByteBuffer rawData, Class<T> messageClass,
			Validator messageValidator, MessageSerDe serDe) {
		this.rawData = rawData;
		this.messageClass = messageClass;
		this.messageValidator = messageValidator;
		this.serDe = serDe;
	}

	/**
	 * Deserializes the given message.
	 * 
	 * @param action
	 * @return
	 * @throws Exception
	 */
	public T deserialize(WebSocketAction action) throws Exception {
		// first deserialize
		T message = null;
		
		if (messageClass != null) {
			message = serDe.deserialize(new ByteBufferBackedInputStream(rawData), messageClass);
		}
		
		// then validate
		if (message != null && action.shouldValidatePayload()) {
			SpringValidatorAdapter validatorAdapter = new SpringValidatorAdapter(messageValidator);
			
			BeanPropertyBindingResult result = new BeanPropertyBindingResult(message, messageClass.getName());
			
			validatorAdapter.validate(message, result);
			
			if (result.hasErrors()) {
				throw new MethodArgumentNotValidException(new MethodParameter(action.getMethod(), action.getPayloadParameterIndex()), result);
			}
		}
		
		return message;
	}
}
