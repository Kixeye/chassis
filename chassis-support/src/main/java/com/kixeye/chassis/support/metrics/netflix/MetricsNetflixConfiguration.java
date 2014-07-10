package com.kixeye.chassis.support.metrics.netflix;

/*
 * #%L
 * Chassis Support
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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.codahale.metrics.MetricRegistry;
import com.netflix.hystrix.strategy.HystrixPlugins;

import de.is24.hystrix.contrib.codahalemetricspublisher.HystrixCodahaleMetricsPublisher;

/**
 * Loads Metrics Netflix configurations.
 * 
 * @author ebahtijaragic
 */
@Configuration
@EnableScheduling
public class MetricsNetflixConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(MetricsNetflixConfiguration.class);
	
	@Autowired
	private MetricRegistry registry;
	
	@PostConstruct
	public void initialize() {
		HystrixCodahaleMetricsPublisher publisher = new HystrixCodahaleMetricsPublisher(registry);
		
		try {
			HystrixPlugins.getInstance().registerMetricsPublisher(publisher);
		} catch (Exception e) {
			logger.error("Another Hystrix metrics publisher was registered. This should never happen, except in tests.", e);
		}
	}

}
