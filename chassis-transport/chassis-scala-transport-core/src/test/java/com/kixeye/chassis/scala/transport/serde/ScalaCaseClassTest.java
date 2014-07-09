package com.kixeye.chassis.scala.transport.serde;

/*
 * #%L
 * Chassis Scala Transport Core
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.kixeye.chassis.transport.serde.converter.BsonJacksonMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.JsonJacksonMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.YamlJacksonMessageSerDe;
import org.apache.commons.io.HexDump;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.serde.converter.ProtobufMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.XmlMessageSerDe;

/**
 * Validates case-class support for various serializers.
 * 
 * @author ebahtijaragic
 */
public class ScalaCaseClassTest {
	private static final Logger logger = LoggerFactory.getLogger(ScalaCaseClassTest.class);
	
	@Test
	public void testJsonSerDe() throws Exception {
		final JsonJacksonMessageSerDe serDe = new JsonJacksonMessageSerDe();

		final TestObject obj = new TestObject(RandomStringUtils.randomAlphanumeric(64), new SomeOtherObject(RandomStringUtils.randomAlphanumeric(64)));
		
		final byte[] serializedObj = serDe.serialize(obj);
		
		dumpToLog(serDe, serializedObj);
		
		final TestObject deserializedObj = serDe.deserialize(serializedObj, 0, serializedObj.length, TestObject.class);
		
		Assert.assertEquals(obj, deserializedObj);
	}
	
	@Test
	@Ignore("Maybe fix this?")
	public void testXmlSerDe() throws Exception {
		final XmlMessageSerDe serDe = new XmlMessageSerDe();

		final TestObject obj = new TestObject(RandomStringUtils.randomAlphanumeric(64), new SomeOtherObject(RandomStringUtils.randomAlphanumeric(64)));
		
		final byte[] serializedObj = serDe.serialize(obj);

		dumpToLog(serDe, serializedObj);
		
		final TestObject deserializedObj = serDe.deserialize(serializedObj, 0, serializedObj.length, TestObject.class);
		
		Assert.assertEquals(obj, deserializedObj);
	}
	
	@Test
	public void testYamlSerDe() throws Exception {
		final YamlJacksonMessageSerDe serDe = new YamlJacksonMessageSerDe();

		final TestObject obj = new TestObject(RandomStringUtils.randomAlphanumeric(64), new SomeOtherObject(RandomStringUtils.randomAlphanumeric(64)));
		
		final byte[] serializedObj = serDe.serialize(obj);

		dumpToLog(serDe, serializedObj);
		
		final TestObject deserializedObj = serDe.deserialize(serializedObj, 0, serializedObj.length, TestObject.class);
		
		Assert.assertEquals(obj, deserializedObj);
	}
	
	@Test
	public void testProtobufSerDe() throws Exception {
		final ProtobufMessageSerDe serDe = new ProtobufMessageSerDe();
		
		final TestObject obj = new TestObject(RandomStringUtils.randomAlphanumeric(64), new SomeOtherObject(RandomStringUtils.randomAlphanumeric(64)));
		
		final byte[] serializedObj = serDe.serialize(obj);

		dumpToLog(serDe, serializedObj);
		
		final TestObject deserializedObj = serDe.deserialize(serializedObj, 0, serializedObj.length, TestObject.class);
		
		Assert.assertEquals(obj, deserializedObj);
	}
	
	@Test
	public void testBsonSerDe() throws Exception {
		final BsonJacksonMessageSerDe serDe = new BsonJacksonMessageSerDe();

		final TestObject obj = new TestObject(RandomStringUtils.randomAlphanumeric(64), new SomeOtherObject(RandomStringUtils.randomAlphanumeric(64)));
		
		final byte[] serializedObj = serDe.serialize(obj);

		dumpToLog(serDe, serializedObj);
		
		final TestObject deserializedObj = serDe.deserialize(serializedObj, 0, serializedObj.length, TestObject.class);
		
		Assert.assertEquals(obj, deserializedObj);
	}
	
	private static void dumpToLog(MessageSerDe serDe, byte[] data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		HexDump.dump(data, 0, baos, 0);
		
		logger.info("Serialized object using [{}] to: \n{}", serDe.getMessageFormatName(), baos.toString(Charsets.UTF_8.name()).trim());
	}
}
