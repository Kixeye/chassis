package com.kixeye.chassis.support.metrics.aws;

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

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.kixeye.chassis.support.util.PropertyUtils;
import com.netflix.config.ConfigurationManager;

/**
 * Spring configuration for publishing Metrics to AWS CloudWatch.
 *
 * @author dturner@kixeye.com
 */
@Configuration
@ComponentScan(basePackageClasses = MetricsCloudWatchConfiguration.class)
public class MetricsCloudWatchConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCloudWatchConfiguration.class);

    public static final String METRICS_AWS_ENABLED_PROPERTY_NAME = "${metrics.aws.enabled}";
    public static String METRICS_AWS_ENABLED;
    public static String METRICS_AWS_FILTER;
    public static String METRICS_AWS_PUBLISH_INTERVAL;
    public static String METRICS_AWS_PUBLISH_INTERVAL_UNIT;

    static {
        METRICS_AWS_ENABLED = PropertyUtils.getPropertyName(METRICS_AWS_ENABLED_PROPERTY_NAME);
        METRICS_AWS_FILTER = PropertyUtils.getPropertyName(MetricsCloudWatchReporter.METRICS_AWS_FILTER_PROPERTY_NAME);
        METRICS_AWS_PUBLISH_INTERVAL = PropertyUtils.getPropertyName(MetricsCloudWatchReporter.METRICS_AWS_PUBLISH_INTERVAL_PROPERTY_NAME);
        METRICS_AWS_PUBLISH_INTERVAL_UNIT = PropertyUtils.getPropertyName(MetricsCloudWatchReporter.METRICS_AWS_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME);
    }

    @Value(METRICS_AWS_ENABLED_PROPERTY_NAME)
    private boolean enabled;

    @Autowired
    private ApplicationContext applicationContext;

    private MetricsCloudWatchReporter reporter;

    @PostConstruct
    public void init() {
        addConfigurationListener();
        if (enabled) {
            createReporter();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MetricsCloudWatchReporter getReporter() {
        return reporter;
    }

    private void createReporter() {
        logger.info("Creating new MetricsCloudWatchReporter...");
        this.reporter = applicationContext.getBean(MetricsCloudWatchReporter.class);
        if (enabled) {
            reporter.start();
        }
    }

    private void destroyReporter() {
        if (this.reporter == null) {
            return;
        }
        this.reporter.stop();
        this.reporter = null;
    }

    private void addConfigurationListener() {
        final MetricsCloudWatchConfiguration springConfig = this;
        ConfigurationManager.getConfigInstance().addConfigurationListener(new ConfigurationListener() {

            @Override
            public synchronized void configurationChanged(ConfigurationEvent event) {
                if (!(event.getType() == AbstractConfiguration.EVENT_SET_PROPERTY ||
                        event.getType() == AbstractConfiguration.EVENT_ADD_PROPERTY)) {
                    return;
                }
                if (event.isBeforeUpdate()) {
                    return;
                }
                String name = event.getPropertyName();

                if (!(name.equals(METRICS_AWS_ENABLED) ||
                        name.equals(METRICS_AWS_FILTER) ||
                        name.equals(METRICS_AWS_PUBLISH_INTERVAL) ||
                        name.equals(METRICS_AWS_PUBLISH_INTERVAL_UNIT))) {
                    return;
                }

                springConfig.enabled = name.equals(METRICS_AWS_ENABLED) ? Boolean.parseBoolean(event.getPropertyValue() + "") : springConfig.enabled;
                destroyReporter();
                if (springConfig.enabled) {
                    createReporter();
                }

            }
        });
    }

}
