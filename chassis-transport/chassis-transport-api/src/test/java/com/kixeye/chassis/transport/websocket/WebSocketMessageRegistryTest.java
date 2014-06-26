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

import org.junit.Assert;
import org.junit.Test;

import com.kixeye.chassis.transport.dto.ServiceError;

/**
 * Tests the {@link WebSocketMessageRegistry}
 * 
 * @author ebahtijaragic
 */
public class WebSocketMessageRegistryTest {
	@Test
	public void testBlankRegistry() {
		final WebSocketMessageRegistry registry = new WebSocketMessageRegistry();
		registry.initialize();
		
		Assert.assertTrue(registry.getTypeIds().contains("error"));
		Assert.assertEquals("error", registry.getTypeIdByClass(ServiceError.class));
		Assert.assertEquals(ServiceError.class, registry.getClassByTypeId("error"));
	}
	
	@Test
	public void testSimpleRegistry() {
		final WebSocketMessageRegistry registry = new WebSocketMessageRegistry();
		registry.initialize();
		
		registry.registerType("testClass1", TestClass1.class);
		registry.registerType("testClass2", TestClass2.class);
		
		Assert.assertTrue(registry.getTypeIds().contains("error"));
		Assert.assertEquals("error", registry.getTypeIdByClass(ServiceError.class));
		Assert.assertEquals(ServiceError.class, registry.getClassByTypeId("error"));

		Assert.assertTrue(registry.getTypeIds().contains("testClass1"));
		Assert.assertEquals("testClass1", registry.getTypeIdByClass(TestClass1.class));
		Assert.assertEquals(TestClass1.class, registry.getClassByTypeId("testClass1"));

		Assert.assertTrue(registry.getTypeIds().contains("testClass2"));
		Assert.assertEquals("testClass2", registry.getTypeIdByClass(TestClass2.class));
		Assert.assertEquals(TestClass2.class, registry.getClassByTypeId("testClass2"));
	}
	
	@Test
	public void testRegistryOverride() {
		final WebSocketMessageRegistry registry = new WebSocketMessageRegistry();
		registry.initialize();

		Assert.assertTrue(registry.getTypeIds().contains("error"));
		Assert.assertEquals("error", registry.getTypeIdByClass(ServiceError.class));
		Assert.assertEquals(ServiceError.class, registry.getClassByTypeId("error"));

		registry.registerType("testClass", TestClass1.class);
		
		Assert.assertTrue(registry.getTypeIds().contains("testClass"));
		Assert.assertEquals("testClass", registry.getTypeIdByClass(TestClass1.class));
		Assert.assertEquals(TestClass1.class, registry.getClassByTypeId("testClass"));

		registry.registerType("testClass", TestClass2.class);
		
		Assert.assertTrue(registry.getTypeIds().contains("testClass"));
		Assert.assertEquals("testClass", registry.getTypeIdByClass(TestClass2.class));
		Assert.assertEquals(TestClass2.class, registry.getClassByTypeId("testClass"));
	}
	
	private static final class TestClass1 {
		
	}

	private static final class TestClass2 {
		
	}
}
