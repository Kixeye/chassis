package com.kixeye.chassis.chassis.logging;

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
