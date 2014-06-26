package com.kixeye.chassis.chassis.events;

import org.springframework.context.ApplicationEvent;

/**
 * An event that gets published when logging is reloaded.
 * 
 * @author ebahtijaragic
 */
public class LoggingReloadedApplicationEvent extends ApplicationEvent {
	private static final long serialVersionUID = 6490124497064298275L;

	public LoggingReloadedApplicationEvent(Object source) {
		super(source);
	}
}
