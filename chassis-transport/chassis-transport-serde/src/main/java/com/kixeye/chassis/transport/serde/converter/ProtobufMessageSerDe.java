package com.kixeye.chassis.transport.serde.converter;

/*
 * #%L
 * Java Transport API
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
import java.io.OutputStream;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.google.common.net.MediaType;
import com.kixeye.chassis.transport.serde.MessageSerDe;

/**
 * JSON-based SerDe.
 * 
 * @author ebahtijaragic
 */
public class ProtobufMessageSerDe implements MessageSerDe {
	private static final String MESSAGE_FORMAT_NAME = "protobuf";
	private static final MediaType[] SUPPORTED_MEDIA_TYPES = new MediaType[] { MediaType.create("application", MESSAGE_FORMAT_NAME) };
	
	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#serialize(java.lang.Object, java.io.OutputStream)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void serialize(Object obj, OutputStream stream) throws IOException {
		Schema schema = RuntimeSchema.getSchema(obj.getClass());

		LinkedBuffer linkedBuffer = LinkedBuffer.allocate(256);
		
		ProtobufIOUtil.writeTo(stream, obj, schema, linkedBuffer);
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#serialize(java.lang.Object)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public byte[] serialize(Object obj) throws IOException {
		Schema schema = RuntimeSchema.getSchema(obj.getClass());

		LinkedBuffer linkedBuffer = LinkedBuffer.allocate(256);
		
		return ProtobufIOUtil.toByteArray(obj, schema, linkedBuffer);
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#deserialize(byte[], int, int, java.lang.Class)
	 */
	public <T> T deserialize(byte[] data, int offset, int length, Class<T> clazz) throws IOException {
		Schema<T> schema = RuntimeSchema.getSchema(clazz);

		T obj = schema.newMessage();

		ProtobufIOUtil.mergeFrom(data, offset, length, obj, schema);
		
		return obj;
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#deserialize(java.io.InputStream, java.lang.Class)
	 */
	public <T> T deserialize(InputStream stream, Class<T> clazz) throws IOException {
		Schema<T> schema = RuntimeSchema.getSchema(clazz);

		T obj = schema.newMessage();

		LinkedBuffer linkedBuffer = LinkedBuffer.allocate(256);
		
		ProtobufIOUtil.mergeFrom(stream, obj, schema, linkedBuffer);
		
		return obj;
	}
	
	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#getSupportedMediaTypes()
	 */
	public MediaType[] getSupportedMediaTypes() {
		return SUPPORTED_MEDIA_TYPES;
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#getMessageFormatName()
	 */
	public String getMessageFormatName() {
		return MESSAGE_FORMAT_NAME;
	}
}
