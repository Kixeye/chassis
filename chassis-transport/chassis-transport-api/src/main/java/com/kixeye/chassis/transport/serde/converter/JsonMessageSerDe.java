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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule$;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * JSON-based SerDe.
 * 
 * @author ebahtijaragic
 */
@Component
public class JsonMessageSerDe implements MessageSerDe {
	private static final String MESSAGE_FORMAT_NAME = "json";
	private static final MediaType[] SUPPORTED_MEDIA_TYPES = new MediaType[] {
        new MediaType("application", MESSAGE_FORMAT_NAME),
		new MediaType("text", MESSAGE_FORMAT_NAME) };

	private final ObjectMapper objectMapper;

    public JsonMessageSerDe() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(DefaultScalaModule$.MODULE$);
        this.objectMapper.registerModule( new GuavaModule() );
        this.objectMapper.registerModule( new JodaModule() );
    }

    public @Autowired(required=false) JsonMessageSerDe( @Qualifier("jacksonScalaObjectMapper") ObjectMapper objectMapper) {
        // assumes the scala portions of the object mapper have already been initialized
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule( new GuavaModule() );
        this.objectMapper.registerModule( new JodaModule() );
    }

    @PostConstruct
    public void initialize() {
        // Empty - Left for legacy calls.
    }

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#serialize(java.lang.Object, java.io.OutputStream)
	 */
	public void serialize(Object obj, OutputStream stream) throws IOException {
		objectMapper.writeValue(stream, obj);
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#serialize(java.lang.Object)
	 */
	public byte[] serialize(Object obj) throws IOException {
		return objectMapper.writeValueAsBytes(obj);
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#deserialize(byte[], int, int, java.lang.Class)
	 */
	public <T> T deserialize(byte[] data, int offset, int length, Class<T> clazz) throws IOException {
		return objectMapper.readValue(data, offset, length, clazz);
	}

	/**
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#deserialize(java.io.InputStream, java.lang.Class)
	 */
	public <T> T deserialize(InputStream stream, Class<T> clazz) throws IOException {
		return objectMapper.readValue(stream, clazz);
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
