package com.kixeye.chassis.transport.http;

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
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.kixeye.chassis.transport.serde.MessageSerDe;

/**
 * A SerDe based message converter.
 * 
 * @author ebahtijaragic
 */
public class SerDeHttpMessageConverter extends AbstractHttpMessageConverter<Object> implements GenericHttpMessageConverter<Object> {
	private final MessageSerDe serDe;
	
	/**
	 * Construct a new {@code ProtostuffJsonHttpMessageConverter}.
	 */
	public SerDeHttpMessageConverter(MessageSerDe serDe) {
		super(convertMediaTypes(serDe.getSupportedMediaTypes()));
		this.serDe = serDe;
	}
	
	/**
	 * @see org.springframework.http.converter.GenericHttpMessageConverter#canRead(java.lang.reflect.Type, java.lang.Class, org.springframework.http.MediaType)
	 */
	public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
		return canRead(mediaType);
	}

	/**
	 * @see org.springframework.http.converter.GenericHttpMessageConverter#read(java.lang.reflect.Type, java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		return serDe.deserialize(inputMessage.getBody(), (Class)type);
	}

	/**
	 * Whether we support this class.
	 * 
	 * @param clazz
	 * @return
	 */
	protected boolean supports(Class<?> clazz) {
		return !(clazz.isPrimitive() || byte[].class.equals(clazz) || ByteBuffer.class.equals(clazz) || String.class.equals(clazz) || InputStream.class.equals(clazz) || Resource.class.equals(clazz));
	}

	/**
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		return serDe.deserialize(inputMessage.getBody(), clazz);
	}

	/**
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
	 */
	protected void writeInternal(Object t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		serDe.serialize(t, outputMessage.getBody());
	}
	
	/**
	 * Converts guava media types to spring media types.
	 * 
	 * @param inputMediaTypes
	 * @return
	 */
	private static MediaType[] convertMediaTypes(com.google.common.net.MediaType[] inputMediaTypes) {
		if (inputMediaTypes == null) {
			return null;
		}
		
		MediaType[] mediaTypes = new MediaType[inputMediaTypes.length];
		
		for (int i = 0; i < inputMediaTypes.length; i++) {
			mediaTypes[i] = new MediaType(inputMediaTypes[i].type(), inputMediaTypes[i].subtype());
		}
		
		return mediaTypes;
	}
}
