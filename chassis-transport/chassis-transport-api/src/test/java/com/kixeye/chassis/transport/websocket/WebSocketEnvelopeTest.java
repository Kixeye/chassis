package com.kixeye.chassis.transport.websocket;

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

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.kixeye.chassis.transport.dto.Envelope;
import com.kixeye.chassis.transport.dto.Header;
import com.kixeye.chassis.transport.util.RandomStringUtils;

/**
 * Tests the {@link WebSocketEnvelope}
 * 
 * @author ebahtijaragic
 */
public class WebSocketEnvelopeTest {
	private static final Random RAND = new Random(WebSocketEnvelope.class.hashCode() + System.nanoTime());
	
	@Test
	public void testWithPayloadNoHeaders() {
		final Envelope baseEnvelope = new Envelope(
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				null, 
				ByteBuffer.wrap(RandomStringUtils.randomString(20, RAND).getBytes()));
		
		final WebSocketEnvelope envelope = new WebSocketEnvelope(baseEnvelope);
		
		Assert.assertEquals(baseEnvelope.action, envelope.getAction());
		Assert.assertEquals(baseEnvelope.typeId, envelope.getTypeId());
		Assert.assertEquals(baseEnvelope.transactionId, envelope.getTransactionId());
		Assert.assertTrue(envelope.hasPayload());
		Assert.assertEquals(baseEnvelope.payload, envelope.getPayload());
		Assert.assertEquals(0, envelope.getHeaders().size());
	}
	
	@Test
	public void testWithPayloadSingleHeaderValue() {
		final String test1Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		
		final Envelope baseEnvelope = new Envelope(
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				Lists.newArrayList(new Header("testName", Lists.newArrayList(test1Value))), 
				ByteBuffer.wrap(RandomStringUtils.randomString(20, RAND).getBytes()));
		
		final WebSocketEnvelope envelope = new WebSocketEnvelope(baseEnvelope);
		
		Assert.assertEquals(baseEnvelope.action, envelope.getAction());
		Assert.assertEquals(baseEnvelope.typeId, envelope.getTypeId());
		Assert.assertEquals(baseEnvelope.transactionId, envelope.getTransactionId());
		Assert.assertTrue(envelope.hasPayload());
		Assert.assertEquals(baseEnvelope.payload, envelope.getPayload());
		Assert.assertEquals(1, envelope.getHeaders().size());
		Assert.assertEquals(test1Value, envelope.getHeaders().get("testName").iterator().next());
	}
	
	@Test
	public void testWithPayloadMultiHeaderValue() {
		final String test1Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		final String test2Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		final String test3Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		
		final Envelope baseEnvelope = new Envelope(
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				Lists.newArrayList(new Header("testName", Lists.newArrayList(test1Value, test2Value, test3Value))), 
				ByteBuffer.wrap(RandomStringUtils.randomString(20, RAND).getBytes()));
		
		final WebSocketEnvelope envelope = new WebSocketEnvelope(baseEnvelope);
		
		Assert.assertEquals(baseEnvelope.action, envelope.getAction());
		Assert.assertEquals(baseEnvelope.typeId, envelope.getTypeId());
		Assert.assertEquals(baseEnvelope.transactionId, envelope.getTransactionId());
		Assert.assertTrue(envelope.hasPayload());
		Assert.assertEquals(baseEnvelope.payload, envelope.getPayload());
		Assert.assertEquals(3, envelope.getHeaders().size());
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test1Value));
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test2Value));
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test3Value));
	}
	
	@Test
	public void testWithPayloadSameHeaderMultiValue() {
		final String test1Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		final String test2Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		final String test3Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		
		final Envelope baseEnvelope = new Envelope(
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				Lists.newArrayList(new Header("testName", Lists.newArrayList(test1Value, test2Value)),
						new Header("testName", Lists.newArrayList(test3Value))), 
				ByteBuffer.wrap(RandomStringUtils.randomString(20, RAND).getBytes()));
		
		final WebSocketEnvelope envelope = new WebSocketEnvelope(baseEnvelope);
		
		Assert.assertEquals(baseEnvelope.action, envelope.getAction());
		Assert.assertEquals(baseEnvelope.typeId, envelope.getTypeId());
		Assert.assertEquals(baseEnvelope.transactionId, envelope.getTransactionId());
		Assert.assertTrue(envelope.hasPayload());
		Assert.assertEquals(baseEnvelope.payload, envelope.getPayload());
		Assert.assertEquals(3, envelope.getHeaders().size());
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test1Value));
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test2Value));
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test3Value));
	}
	
	@Test
	public void testWithPayloadAdditiveHeaderMultiValue() {
		final String test1Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		final String test2Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		final String test3Value = RandomStringUtils.randomAlphaNumericString(20, RAND);
		
		final Envelope baseEnvelope = new Envelope(
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				Lists.newArrayList(new Header("testName", Lists.newArrayList(test1Value, test2Value)),
						new Header("testName", Lists.newArrayList(test1Value, test2Value, test3Value))), 
				ByteBuffer.wrap(RandomStringUtils.randomString(20, RAND).getBytes()));
		
		final WebSocketEnvelope envelope = new WebSocketEnvelope(baseEnvelope);
		
		Assert.assertEquals(baseEnvelope.action, envelope.getAction());
		Assert.assertEquals(baseEnvelope.typeId, envelope.getTypeId());
		Assert.assertEquals(baseEnvelope.transactionId, envelope.getTransactionId());
		Assert.assertTrue(envelope.hasPayload());
		Assert.assertEquals(baseEnvelope.payload, envelope.getPayload());
		Assert.assertEquals(3, envelope.getHeaders().size());
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test1Value));
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test2Value));
		Assert.assertTrue(envelope.getHeaders().containsEntry("testName", test3Value));
	}
	
	@Test
	public void testWithNoPayload() {
		final Envelope baseEnvelope = new Envelope(
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				RandomStringUtils.randomAlphaNumericString(20, RAND), 
				null, 
				null);
		
		final WebSocketEnvelope envelope = new WebSocketEnvelope(baseEnvelope);
		
		Assert.assertEquals(baseEnvelope.action, envelope.getAction());
		Assert.assertEquals(baseEnvelope.typeId, envelope.getTypeId());
		Assert.assertEquals(baseEnvelope.transactionId, envelope.getTransactionId());
		Assert.assertFalse(envelope.hasPayload());
		Assert.assertEquals(baseEnvelope.payload, envelope.getPayload());
	}
}
