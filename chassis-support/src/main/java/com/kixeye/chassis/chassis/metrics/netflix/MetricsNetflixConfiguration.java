package com.kixeye.chassis.chassis.metrics.netflix;

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
