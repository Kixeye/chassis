package com.kixeye.chassis.support.logging;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.flume.agent.embedded.EmbeddedAgent;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.SystemPropertyUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.google.common.collect.Sets;
import com.kixeye.chassis.support.events.LoggingReloadedApplicationEvent;

/**
 * Loads flume into our logger.
 * 
 * @author ebahtijaragic
 */
@Component(FlumeLoggerLoader.PROTOTYPE_BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FlumeLoggerLoader {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FlumeLoggerLoader.class);
	
	public static final String PROTOTYPE_BEAN_NAME = "flumeLoggerLoader";
	public static final String FLUME_LOGGER_ENABLED_PROPERTY = "logging.flume.enabled";

	public static final Set<String> RELOAD_PROPERTIES = Sets.newHashSet("logging.flume.serversUsage", "logging.flume.servers");

	private String name;
	private EmbeddedAgent agent;
	private FlumeLogAppender appender;

    @Value("${app.name}")
    private String serviceName;

    @Value("${logging.flume.servers}")
    private List<String> servers;

    @Value("${logging.flume.serversUsage:failover}")
    private String serversUsage;

	@Autowired
	private ApplicationEventMulticaster multicaster;
	
	public FlumeLoggerLoader(String name) {
		this.name = name;
	}

	@PostConstruct
	public void initialize() {
		multicaster.addApplicationListener(loggingListener);
		
		ConcurrentHashMap<String, String> flumeConfig = new ConcurrentHashMap<>();
		
		List<String> sinks = new ArrayList<>();
		
		for (String server : servers) {
			String sinkName = server.replace(":", "-").replace(".", "_");
			
			String[] servers = server.split(":", 2);

			if (servers.length == 2) {
				flumeConfig.put(sinkName + ".type", "avro");
				flumeConfig.put(sinkName + ".channels", "channel-" + name);
				flumeConfig.put(sinkName + ".hostname", servers[0]);
				flumeConfig.put(sinkName + ".port", servers[1]);
			} else {
				logger.error("Invalid server format [{}], should be [hostname:port]", server);
			}
			
			sinks.add(sinkName);
		}
		
		// force some properties
		flumeConfig.put("channel.type", "file");
		flumeConfig.put("sinks", StringUtils.collectionToDelimitedString(sinks, " "));
		flumeConfig.putIfAbsent("processor.type", serversUsage);
		flumeConfig.put("channel.checkpointDir", SystemPropertyUtils.resolvePlaceholders("${user.dir}/flume-data/checkpoint"));
		flumeConfig.put("channel.dataDirs", SystemPropertyUtils.resolvePlaceholders("${user.dir}/flume-data/data"));
		
		agent = new EmbeddedAgent(name);
		agent.configure(flumeConfig);
		agent.start();
		
		appender = new FlumeLogAppender(agent, serviceName);
		
		installFlumeAppender();
	}
	
	@PreDestroy
	public void destroy() {
		if (agent != null) {
			agent.stop();
			agent = null;
		}
		
		multicaster.removeApplicationListener(loggingListener);
		uninstallFlumeAppender();
	}
	
	public synchronized void installFlumeAppender() {
		LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		appender.setContext(logContext);
		appender.start();

		Logger logger = logContext.getLogger(Logger.ROOT_LOGGER_NAME);

		if (logger.getAppender(appender.getName()) == null) {
			logger.addAppender(appender);
		}
	}
	
	public synchronized void uninstallFlumeAppender() {
		LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		appender.setContext(logContext);
		appender.stop();
		
		Logger logger = logContext.getLogger(Logger.ROOT_LOGGER_NAME);
		
		if (logger.getAppender(appender.getName()) != null) {
			logger.detachAppender(appender);
		}
	}
	
	public ApplicationListener<LoggingReloadedApplicationEvent> loggingListener = new ApplicationListener<LoggingReloadedApplicationEvent>() {
		/**
		 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
		 */
		public void onApplicationEvent(LoggingReloadedApplicationEvent event) {
			installFlumeAppender();
		}
	};
}
