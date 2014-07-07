
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

package com.kixeye.chassis.transport.serde.bson;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.io.IOContext;

import de.undercouch.bson4jackson.BsonParser;

/**
 * Fixes byte array issue with Bson parsing.
 * 
 * @author ebahtijaragic
 */
public class KixeyeBsonParser extends BsonParser {
	public KixeyeBsonParser(IOContext ctxt, int jsonFeatures, int bsonFeatures, InputStream in) {
		super(ctxt, jsonFeatures, bsonFeatures, in);
	}

	@Override
	public byte[] getBinaryValue(Base64Variant b64variant) throws IOException, JsonParseException {
		Object obj = getEmbeddedObject();
		if (obj instanceof byte[]) {
			return (byte[])obj;
		} else {
			return getText().getBytes();
		}
	}
}
