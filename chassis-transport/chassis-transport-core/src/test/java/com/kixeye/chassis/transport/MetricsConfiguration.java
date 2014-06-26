package com.kixeye.chassis.transport;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

/**
 * Configure metrics.
 * 
 * @author ebahtijaragic
 */
@Configuration
public class MetricsConfiguration {
	/***
     * Initializes the metrics registry
     *
     * @return metric registry bean
     */
	@Bean
	public MetricRegistry metricRegistry() {
		final MetricRegistry bean = new MetricRegistry();

        // add JVM metrics
		bean.register("jvm.gc", new GarbageCollectorMetricSet());
		bean.register("jvm.memory", new MemoryUsageGaugeSet());
		bean.register("jvm.thread-states", new ThreadStatesGaugeSet());
		bean.register("jvm.fd", new FileDescriptorRatioGauge());

		return bean;
	}

    /***
     * Initializes the health check registry
     *
     * @return health check registry bean
     */
	@Bean
	public HealthCheckRegistry healthCheckRegistry(ApplicationContext context) {
		final HealthCheckRegistry bean = new HealthCheckRegistry();

        // auto-register beans implementing health checks
        Map<String,HealthCheck> healthChecks = context.getBeansOfType(HealthCheck.class);
        for (HealthCheck check : healthChecks.values()) {
            bean.register( check.getClass().getName(), check );
        }

        return bean;
    }
}
