package com.kixeye.chassis.bootstrap.spring;

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
