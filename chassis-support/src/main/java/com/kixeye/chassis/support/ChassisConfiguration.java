package com.kixeye.chassis.support;

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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.logback.InstrumentedAppender;
import com.kixeye.chassis.support.eureka.EurekaConfiguration;
import com.kixeye.chassis.support.logging.LoggingConfiguration;
import com.kixeye.chassis.support.metrics.aws.MetricsCloudWatchConfiguration;
import com.kixeye.chassis.support.metrics.codahale.MetricsGraphiteConfiguration;
import com.kixeye.chassis.support.metrics.netflix.MetricsNetflixConfiguration;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckCallback;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DiscoveryManager;

/**
 * The main Spring configuration class for the Chassis.
 */
@Configuration
@EnableAspectJAutoProxy
@PropertySource("classpath:/chassis-default.properties")
@Import({
    EurekaConfiguration.class, LoggingConfiguration.class,
	MetricsGraphiteConfiguration.class, MetricsCloudWatchConfiguration.class, MetricsNetflixConfiguration.class})
public class ChassisConfiguration implements ApplicationListener<ApplicationEvent> {
	
	@Value("${chassis.eureka.disable}")
	private boolean disableEureka;

	@Value("${eureka.datacenter}")
	private String datacenter;

    @Autowired
    private ApplicationContext thisApplicationContext;

    @Override
	public void onApplicationEvent(ApplicationEvent event) {
        //we only want to tell Eureka that the application up
        //when the root application context (thisApplicationContext) has
        //been fully started.  we want to ignore any ContextRefreshedEvent
        //from child application contexts.
        if(!event.getSource().equals(thisApplicationContext)){
            return;
        }
		if (event instanceof ContextRefreshedEvent) {
            if (!disableEureka) {
                // tell Eureka the server UP which in turn starts the health checks and heartbeat
                ApplicationInfoManager.getInstance().setInstanceStatus(InstanceStatus.UP);
            }
		} else if (event instanceof ContextClosedEvent) {
            if (!disableEureka) {
                ApplicationInfoManager.getInstance().setInstanceStatus(InstanceStatus.DOWN);
            }
		}
	}


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
		bean.register("jvm.load-average", new Gauge<Double>() {
			private OperatingSystemMXBean mxBean = ManagementFactory.getOperatingSystemMXBean();
			
			public Double getValue() {
				try {
					return mxBean.getSystemLoadAverage();
				} catch (Exception e) {
					// not supported
					return -1d;
				}
			}
		});

		// add Logback metrics
		final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
		final Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);
		final InstrumentedAppender appender = new InstrumentedAppender(bean);
		appender.setContext(root.getLoggerContext());
		appender.start();
		root.addAppender(appender);

		return bean;
	}

    /***
     * Initializes the health check registry
     *
     * @return health check registry bean
     */
	@Bean
	public HealthCheckRegistry healthCheckRegistry(ApplicationContext context, DiscoveryManager eureka) {
		final HealthCheckRegistry bean = new HealthCheckRegistry();

        // auto-register beans implementing health checks
        Map<String,HealthCheck> healthChecks = context.getBeansOfType(HealthCheck.class);
        for (HealthCheck check : healthChecks.values()) {
            bean.register( check.getClass().getName(), check );
        }

        // connect health checks into Eureka
		if (!disableEureka) {
			eureka.getDiscoveryClient().registerHealthCheckCallback(
					new HealthCheckCallback() {
						@Override
						public boolean isHealthy() {
							for (Entry<String, HealthCheck.Result> entry : bean.runHealthChecks().entrySet()) {
								if (!entry.getValue().isHealthy()) {
									return false;
								}
							}
							return true;
						}
					});
		}

        return bean;
    }
}
