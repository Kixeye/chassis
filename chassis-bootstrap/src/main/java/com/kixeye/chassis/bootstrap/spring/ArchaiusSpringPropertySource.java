package com.kixeye.chassis.bootstrap.spring;

/*
 * #%L
 * Chassis Bootstrap
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.EnumerablePropertySource;

import com.google.common.collect.Iterators;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;

/**
 * A spring property source that uses archaius.
 * 
 * @author ebahtijaragic
 */
public class ArchaiusSpringPropertySource extends EnumerablePropertySource<Map<String, Object>> {
	public ArchaiusSpringPropertySource(String name) {
		super(name, new HashMap<String, Object>());
	}

	@Override
	public String[] getPropertyNames() {
		return Iterators.toArray(ConfigurationManager.getConfigInstance().getKeys(), String.class);
	}

	@Override
	public Object getProperty(String name) {
		return DynamicPropertyFactory.getInstance().getStringProperty(name, null).get();
	}
}
