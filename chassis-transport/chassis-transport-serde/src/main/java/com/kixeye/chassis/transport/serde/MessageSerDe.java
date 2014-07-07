package com.kixeye.chassis.transport.serde;

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

import org.springframework.http.MediaType;

/**
 * A message converter.
 * 
 * @author ebahtijaragic
 */
public interface MessageSerDe {
	/**
	 * Serializes an object into the given stream.
	 * 
	 * @param obj
	 * @param stream
	 * @throws IOException
	 */
	public void serialize(Object obj, OutputStream stream) throws IOException;

	/**
	 * Serializes an object into the given stream.
	 * 
	 * @param obj
	 * @param stream
	 * @throws IOException
	 */
	public byte[] serialize(Object obj) throws IOException;
	
	/**
	 * Deserializes data into an object.
	 * 
	 * @param data
	 * @param offset
	 * @param length
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	public <T> T deserialize(byte[] data, int offset, int length, Class<T> clazz) throws IOException;

	/**
	 * Deserializes data into an object.
	 * 
	 * @param stream
	 * @param clazz
	 * @return
	 */
	public <T> T deserialize(InputStream stream, Class<T> clazz) throws IOException;
	
	/**
	 * Gets the name of the message format.
	 * 
	 * @return
	 */
	public String getMessageFormatName();
	
	/**
	 * Gets the list of supported media types.
	 * 
	 * @return
	 */
	public MediaType[] getSupportedMediaTypes();
}
