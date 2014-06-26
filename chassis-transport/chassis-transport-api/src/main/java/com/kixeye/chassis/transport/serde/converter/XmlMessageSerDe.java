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

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * XML-based Message SerDe
 * 
 * @author ebahtijaragic
 */
@Component
public class XmlMessageSerDe implements MessageSerDe {
	private static final String MESSAGE_FORMAT_NAME = "xml";
	private static final MediaType[] SUPPORTED_MEDIA_TYPES = new MediaType[] { new MediaType("application", MESSAGE_FORMAT_NAME), 
		new MediaType("text", MESSAGE_FORMAT_NAME) };

	private final XStream xstream = new XStream(new StaxDriver());

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#serialize(java.lang.Object, java.io.OutputStream)
	 */
	public void serialize(Object obj, OutputStream stream) throws IOException {
		xstream.toXML(obj, stream);
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#serialize(java.lang.Object)
	 */
	public byte[] serialize(Object obj) throws IOException {
		return xstream.toXML(obj).getBytes(Charsets.UTF_8);
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#deserialize(byte[], int, int, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T> T deserialize(byte[] data, int offset, int length, Class<T> clazz) throws IOException {
		try {
			return (T)xstream.fromXML(new String(data, offset, length, Charsets.UTF_8), clazz.newInstance());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#deserialize(java.io.OutputStream, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T> T deserialize(InputStream stream, Class<T> clazz) throws IOException {
		try {
			return (T)xstream.fromXML(stream, clazz.newInstance());
		} catch (Exception e) {
			throw new IOException(e);
		}
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
