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

import java.util.HashMap;
import java.util.Map;

import org.apache.flume.agent.embedded.EmbeddedAgent;
import org.apache.flume.event.SimpleEvent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.google.common.base.Charsets;
import com.kixeye.chassis.chassis.util.NetworkingUtils;

/**
 * A logback appender for flume.
 * 
 * @author ebahtijaragic
 */
public class FlumeLogAppender extends AppenderBase<ILoggingEvent> {
	private final EmbeddedAgent agent;

	private final String product;
	
	public FlumeLogAppender(EmbeddedAgent agent, String product) {
		this.agent = agent;
		this.product = product;
		this.setName("flumeAppender");
	}
	
	@Override
	protected void append(ILoggingEvent logEvent) {
		SimpleEvent flumeEvent = new SimpleEvent();
		
		Map<String, String> headers = new HashMap<>();
		headers.put("timestamp", "" + logEvent.getTimeStamp());
		headers.put("level", logEvent.getLevel().levelStr);
		headers.put("threadName", logEvent.getThreadName());
		headers.put("loggerName", logEvent.getLoggerName());
		headers.put("product", product);
		headers.put("hostname", NetworkingUtils.getIpAddress());
		flumeEvent.setHeaders(headers);
		flumeEvent.setBody(logEvent.getFormattedMessage().getBytes(Charsets.UTF_8));
		
		try {
			agent.put(flumeEvent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
