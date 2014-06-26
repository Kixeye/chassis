package com.kixeye.chassis.chassis.metrics.codahale;

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

import java.net.UnknownHostException;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.kixeye.chassis.chassis.util.PropertyUtils;
import com.netflix.config.ConfigurationManager;

@Configuration
@ComponentScan(basePackageClasses = MetricsGraphiteConfiguration.class)
public class MetricsGraphiteConfiguration {

    public static final String METRICS_GRAPHITE_ENABLED_PROPERTY_NAME = "${metrics.graphite.enabled}";
    private static String METRICS_GRAPHITE_ENABLED;
    private static String METRICS_GRAPHITE_SERVER;
    private static String METRICS_GRAPHITE_PORT;
    private static String METRICS_GRAPHITE_FILTER;
    private static String METRICS_GRAPHITE_PUBLISH_INTERVAL;
    private static String METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT;
    @Value(METRICS_GRAPHITE_ENABLED_PROPERTY_NAME)
    private boolean enabled;
    @Autowired
    private ApplicationContext applicationContext;
    private MetricsGraphiteReporterLoader reporterLoader;

    static {
        METRICS_GRAPHITE_ENABLED = PropertyUtils.getPropertyName(METRICS_GRAPHITE_ENABLED_PROPERTY_NAME);
        METRICS_GRAPHITE_SERVER = PropertyUtils.getPropertyName(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_SERVER_PROPERTY_NAME);
        METRICS_GRAPHITE_PORT = PropertyUtils.getPropertyName(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PORT_PROPERTY_NAME);
        METRICS_GRAPHITE_FILTER = PropertyUtils.getPropertyName(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_FILTER_PROPERTY_NAME);
        METRICS_GRAPHITE_PUBLISH_INTERVAL = PropertyUtils.getPropertyName(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PUBLISH_INTERVAL_PROPERTY_NAME);
        METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT = PropertyUtils.getPropertyName(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME);
    }

    @PostConstruct
    public void init() throws UnknownHostException {
        addConfigurationListener();
        if (enabled) {
            createReporterLoader();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MetricsGraphiteReporterLoader getReporterLoader() {
        return reporterLoader;
    }

    private void createReporterLoader() {
        reporterLoader = applicationContext.getBean(MetricsGraphiteReporterLoader.class);
        if (enabled) {
            reporterLoader.start();
        }
    }

    private void destroyReporterLoader() {
        if (reporterLoader == null) {
            return;
        }
        reporterLoader.stop();
        reporterLoader = null;
    }

    private void addConfigurationListener() {
        final MetricsGraphiteConfiguration springConfig = this;
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
                if (!(name.equals(METRICS_GRAPHITE_ENABLED) ||
                        name.equals(METRICS_GRAPHITE_SERVER) ||
                        name.equals(METRICS_GRAPHITE_PORT) ||
                        name.equals(METRICS_GRAPHITE_FILTER) ||
                        name.equals(METRICS_GRAPHITE_PUBLISH_INTERVAL) ||
                        name.equals(METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT))) {
                    return;
                }

                springConfig.enabled = name.equals(METRICS_GRAPHITE_ENABLED) ? Boolean.parseBoolean(event.getPropertyValue() + "") : springConfig.enabled;

                destroyReporterLoader();
                if (springConfig.enabled) {
                    createReporterLoader();
                }

            }
        });
    }

}
