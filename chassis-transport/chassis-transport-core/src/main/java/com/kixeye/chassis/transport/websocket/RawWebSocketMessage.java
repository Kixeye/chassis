package com.kixeye.chassis.transport.websocket;

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
