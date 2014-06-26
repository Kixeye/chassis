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

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule$;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.serde.bson.KixeyeBsonParser;
import de.undercouch.bson4jackson.BsonFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * BSON-based SerDe.
 * 
 * @author ebahtijaragic
 */
@Component
public class BsonMessageSerDe implements MessageSerDe {
	private static final String MESSAGE_FORMAT_NAME = "bson";
	private static final MediaType[] SUPPORTED_MEDIA_TYPES = new MediaType[] { new MediaType("application", MESSAGE_FORMAT_NAME) };

	private ObjectMapper objectMapper = new ObjectMapper(new BsonFactory() {
		private static final long serialVersionUID = 1937650622229505600L;
		
		@Override
		protected KixeyeBsonParser _createParser(InputStream in, IOContext ctxt) {
			KixeyeBsonParser p = new KixeyeBsonParser(ctxt, _parserFeatures, _bsonParserFeatures, in);
			ObjectCodec codec = getCodec();
			if (codec != null) {
				p.setCodec(codec);
			}
			return p;
		}
	});

    @PostConstruct
    public void initialize() {
        objectMapper.registerModule( DefaultScalaModule$.MODULE$ );
        objectMapper.registerModule( new GuavaModule() );
        objectMapper.registerModule( new JodaModule() );
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
	 * @see com.kixeye.chassis.transport.serde.MessageSerDe#deserialize(java.io.OutputStream, java.lang.Class)
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
