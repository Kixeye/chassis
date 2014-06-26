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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.HexDump;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.kixeye.chassis.transport.util.RandomStringUtils;
import com.kixeye.chassis.transport.websocket.WebSocketEnvelope;

/**
 * Tests the various serdes.
 * 
 * @author ebahtijaragic
 */
public class SerDeTest {
	private static final Logger logger = LoggerFactory.getLogger(SerDeTest.class);
	
	private static final Random RAND = new Random(WebSocketEnvelope.class.hashCode() + System.nanoTime());
	
	private AbstractApplicationContext context;
	
	@Before
	public void beforeTest() {
		context = new AnnotationConfigApplicationContext(SerDeConfiguration.class);
	}
	
	@After
	public void afterTest() {
		if (context != null) {
			context.close();
			context = null;
		}
	}
	
	@Test
	public void testSerDes() throws Exception {
		Collection<MessageSerDe> serDes = context.getBeansOfType(MessageSerDe.class).values();
		
		for (MessageSerDe serDe : serDes) {
			logger.info("Testing SerDe: [{}], for serialization of: [{}]", serDe.getClass().getSimpleName(), serDe.getMessageFormatName());
			
			TestObject testObject = TestObject.generateRandom(1, true);
			
			logger.info("Generated test object for serialization: [{}]", testObject);
			
			// serialize object
			byte[] serializedTestObject = serDe.serialize(testObject);
			dumpBytes(serializedTestObject);
			
			// test deserialization
			Assert.assertEquals(testObject, serDe.deserialize(serializedTestObject, 0, serializedTestObject.length, TestObject.class));
			Assert.assertEquals(testObject, serDe.deserialize(new ByteArrayInputStream(serializedTestObject), TestObject.class));
			
			// serialize object
			ByteArrayOutputStream serializedTestObjectStream = new ByteArrayOutputStream();
			serDe.serialize(testObject, serializedTestObjectStream);
			
			serializedTestObject = serializedTestObjectStream.toByteArray();
			dumpBytes(serializedTestObject);
			
			// test deserialization
			Assert.assertEquals(testObject, serDe.deserialize(serializedTestObject, 0, serializedTestObject.length, TestObject.class));
			Assert.assertEquals(testObject, serDe.deserialize(new ByteArrayInputStream(serializedTestObject), TestObject.class));
		}
	}
	
	public static class TestObject {
		public TestObject sampleInnerObject;
		
		public List<TestObject> sampleInnerObjectList;
		
		public ByteBuffer sampleByteBuffer;
		
		public String sampleString;
		public boolean sampleBoolean;
		public byte sampleByte;
		public char sampleChar;
		public int sampleInt;
		public long sampleLong;

		public String[] sampleStringArray;
		public boolean[] sampleBooleanArray;
		public byte[] sampleByteArray;
		public char[] sampleCharArray;
		public int[] sampleIntArray;
		public long[] sampleLongArray;
		
		public TestObject() {
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (sampleBoolean ? 1231 : 1237);
			result = prime * result + Arrays.hashCode(sampleBooleanArray);
			result = prime * result + sampleByte;
			result = prime * result + Arrays.hashCode(sampleByteArray);
			result = prime
					* result
					+ ((sampleByteBuffer == null) ? 0 : sampleByteBuffer
							.hashCode());
			result = prime * result + sampleChar;
			result = prime * result + Arrays.hashCode(sampleCharArray);
			result = prime
					* result
					+ ((sampleInnerObject == null) ? 0 : sampleInnerObject
							.hashCode());
			result = prime
					* result
					+ ((sampleInnerObjectList == null) ? 0
							: sampleInnerObjectList.hashCode());
			result = prime * result + sampleInt;
			result = prime * result + Arrays.hashCode(sampleIntArray);
			result = prime * result + (int) (sampleLong ^ (sampleLong >>> 32));
			result = prime * result + Arrays.hashCode(sampleLongArray);
			result = prime * result
					+ ((sampleString == null) ? 0 : sampleString.hashCode());
			result = prime * result + Arrays.hashCode(sampleStringArray);
			return result;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestObject other = (TestObject) obj;
			if (sampleBoolean != other.sampleBoolean)
				return false;
			if (!Arrays.equals(sampleBooleanArray, other.sampleBooleanArray))
				return false;
			if (sampleByte != other.sampleByte)
				return false;
			if (!Arrays.equals(sampleByteArray, other.sampleByteArray))
				return false;
			if (sampleByteBuffer == null) {
				if (other.sampleByteBuffer != null)
					return false;
			} else if (!sampleByteBuffer.equals(other.sampleByteBuffer))
				return false;
			if (sampleChar != other.sampleChar)
				return false;
			if (!Arrays.equals(sampleCharArray, other.sampleCharArray))
				return false;
			if (sampleInnerObject == null) {
				if (other.sampleInnerObject != null)
					return false;
			} else if (!sampleInnerObject.equals(other.sampleInnerObject))
				return false;
			if (sampleInnerObjectList == null) {
				if (other.sampleInnerObjectList != null)
					return false;
			} else if (!sampleInnerObjectList
					.equals(other.sampleInnerObjectList))
				return false;
			if (sampleInt != other.sampleInt)
				return false;
			if (!Arrays.equals(sampleIntArray, other.sampleIntArray))
				return false;
			if (sampleLong != other.sampleLong)
				return false;
			if (!Arrays.equals(sampleLongArray, other.sampleLongArray))
				return false;
			if (sampleString == null) {
				if (other.sampleString != null)
					return false;
			} else if (!sampleString.equals(other.sampleString))
				return false;
			if (!Arrays.equals(sampleStringArray, other.sampleStringArray))
				return false;
			return true;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "TestObject [sampleInnerObject=" + sampleInnerObject
					+ ", sampleInnerObjectList=" + sampleInnerObjectList
					+ ", sampleByteBuffer=" + sampleByteBuffer
					+ ", sampleString=" + sampleString + ", sampleBoolean="
					+ sampleBoolean + ", sampleByte=" + sampleByte
					+ ", sampleChar=" + sampleChar + ", sampleInt=" + sampleInt
					+ ", sampleLong=" + sampleLong + ", sampleStringArray="
					+ Arrays.toString(sampleStringArray)
					+ ", sampleBooleanArray="
					+ Arrays.toString(sampleBooleanArray)
					+ ", sampleByteArray=" + Arrays.toString(sampleByteArray)
					+ ", sampleCharArray=" + Arrays.toString(sampleCharArray)
					+ ", sampleIntArray=" + Arrays.toString(sampleIntArray)
					+ ", sampleLongArray=" + Arrays.toString(sampleLongArray)
					+ "]";
		}

		/**
		 * Generates a random object.
		 * 
		 * @return
		 */
		public static TestObject generateRandom(int innerObjectCount, boolean createInnerObjectList) {
			TestObject obj = new TestObject();
			
			if (innerObjectCount > 0) {
				obj.sampleInnerObject = generateRandom(innerObjectCount - 1, false);
			}
			
			if (createInnerObjectList) {
				obj.sampleInnerObjectList = Lists.newArrayList(generateRandom(0, false), generateRandom(0, false));
			}
			
			obj.sampleString = RandomStringUtils.randomAlphaNumericString(30, RAND);
			obj.sampleBoolean = RAND.nextBoolean();
			obj.sampleByte = (byte)RAND.nextInt();
			obj.sampleChar = RandomStringUtils.randomAlphaNumericString(1, RAND).charAt(0);
			obj.sampleInt = RAND.nextInt();
			obj.sampleLong = RAND.nextInt();
			
			byte[] sampleByteBufferArr = new byte[32];
			RAND.nextBytes(sampleByteBufferArr);
			
			obj.sampleByteBuffer = ByteBuffer.wrap(sampleByteBufferArr);

			obj.sampleStringArray = new String[] { RandomStringUtils.randomAlphaNumericString(30, RAND), RandomStringUtils.randomAlphaNumericString(30, RAND) };
			obj.sampleBooleanArray = new boolean[] { RAND.nextBoolean(), RAND.nextBoolean() };
			obj.sampleByteArray = new byte[30];
			RAND.nextBytes(obj.sampleByteArray);
			obj.sampleCharArray = new char[] { RandomStringUtils.randomAlphaNumericString(1, RAND).charAt(0), RandomStringUtils.randomAlphaNumericString(1, RAND).charAt(0) };
			obj.sampleIntArray = new int[] { RAND.nextInt(), RAND.nextInt() };
			obj.sampleLongArray = new long[] { RAND.nextInt(), RAND.nextInt() };
			
			return obj;
		}
	}
	
	private static void dumpBytes(byte[] bytes) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		HexDump.dump(bytes, 0, baos, 0);
		logger.info("Serialized object to: \n{}", baos.toString(Charsets.UTF_8.name()).trim());
	}
}
