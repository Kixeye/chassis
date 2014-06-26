package com.kixeye.chassis.chassis.logging;

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

import java.io.ByteArrayInputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.base.Charsets;
import com.kixeye.chassis.chassis.events.LoggingReloadedApplicationEvent;
import com.netflix.config.ConfigurationManager;

/**
 * Configures logging.
 * 
 * @author ebahtijaragic
 */
@Configuration
@ComponentScan(basePackageClasses={LoggingConfiguration.class})
public class LoggingConfiguration {
	private static final String LOGBACK_CONFIG_NAME = "logback.xml";
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@PostConstruct
	public void initialize() {
		AbstractConfiguration config = ConfigurationManager.getConfigInstance();

		if (config.containsKey(LOGBACK_CONFIG_NAME)) {
			System.out.println("Loading logging config.");
			
			reloadLogging(config.getString(LOGBACK_CONFIG_NAME));
		}
		
		config.addConfigurationListener(new ConfigurationListener() {
			@Override
			public synchronized void configurationChanged(ConfigurationEvent event) {
				if ((event.getType() == AbstractConfiguration.EVENT_ADD_PROPERTY || event.getType() == AbstractConfiguration.EVENT_SET_PROPERTY) && 
					StringUtils.equalsIgnoreCase(LOGBACK_CONFIG_NAME, event.getPropertyName()) && event.getPropertyValue() != null && !event.isBeforeUpdate()) {
					System.out.println("Reloading logging config.");
					
					reloadLogging((String)event.getPropertyValue());
				}
			}
		});

        ConfigurationListener flumeConfigListener = new ConfigurationListener() {
            private FlumeLoggerLoader loggerLoader = null;

            public synchronized void configurationChanged(ConfigurationEvent event) {
                if (!(event.getType() == AbstractConfiguration.EVENT_SET_PROPERTY || event.getType() == AbstractConfiguration.EVENT_ADD_PROPERTY ||
                        event.getType() == AbstractConfiguration.EVENT_CLEAR_PROPERTY)) {
                    return;
                }
                
                if (FlumeLoggerLoader.FLUME_LOGGER_ENABLED_PROPERTY.equals(event.getPropertyName())) {
	                if ("true".equals(event.getPropertyValue())) {
	                    if (loggerLoader == null) {
	                        // construct the bean
	                        loggerLoader = (FlumeLoggerLoader) applicationContext.getBean(FlumeLoggerLoader.PROTOTYPE_BEAN_NAME, "chassis");
	                    } // else we already have one so we're cool
	                } else {
	                    if (loggerLoader != null) {
	                        // delete the bean
	                        ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) applicationContext.getParentBeanFactory();
	                        beanFactory.destroyBean(FlumeLoggerLoader.PROTOTYPE_BEAN_NAME, loggerLoader);
	                        loggerLoader = null;
	                    } // else we don't have any so we're cool
	                }
                } else if (FlumeLoggerLoader.RELOAD_PROPERTIES.contains(event.getPropertyValue())) {
                	// only reload if we're already running - otherwise ignore
                	if (loggerLoader != null) {
                        // delete the bean
                        ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) applicationContext.getParentBeanFactory();
                        beanFactory.destroyBean(FlumeLoggerLoader.PROTOTYPE_BEAN_NAME, loggerLoader);
                        loggerLoader = null;

                        // construct the bean
                        loggerLoader = (FlumeLoggerLoader) applicationContext.getBean(FlumeLoggerLoader.PROTOTYPE_BEAN_NAME, "chassis");
                    } // else we don't have any so we're cool
                }
            }
        };

        config.addConfigurationListener(flumeConfigListener);
		
		flumeConfigListener.configurationChanged(new ConfigurationEvent(this, AbstractConfiguration.EVENT_SET_PROPERTY, FlumeLoggerLoader.FLUME_LOGGER_ENABLED_PROPERTY, 
				config.getProperty(FlumeLoggerLoader.FLUME_LOGGER_ENABLED_PROPERTY), false));
	}
	
	/**
	 * Reloads logging.
	 * 
	 * @param logbackConfig XML containing the logback configuration
	 */
	public void reloadLogging(String logbackConfig) {
		LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(logContext);
			logContext.reset();
			configurator.doConfigure(new ByteArrayInputStream(logbackConfig.getBytes(Charsets.UTF_8)));
		} catch (JoranException je) {
			// StatusPrinter will handle this
		} catch (Exception ex) {
			ex.printStackTrace(); // Just in case, so we see a stacktrace
		}
		
		StatusPrinter.printInCaseOfErrorsOrWarnings(logContext);
		
		applicationContext.publishEvent(new LoggingReloadedApplicationEvent(this));
	}
}
