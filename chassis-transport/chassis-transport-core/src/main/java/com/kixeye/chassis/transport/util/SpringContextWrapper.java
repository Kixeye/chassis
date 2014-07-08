package com.kixeye.chassis.transport.util;

import org.springframework.context.ApplicationContext;

/**
 * Wraps an {@link ApplicationContext}.
 * 
 * @author ebahtijaragic
 */
public class SpringContextWrapper {
	private ApplicationContext context;

	/**
	 * @param context
	 */
	public SpringContextWrapper(ApplicationContext context) {
		this.context = context;
	}

	/**
	 * @return the context
	 */
	public ApplicationContext getContext() {
		return context;
	}

	/**
	 * @param context
	 *            the context to set
	 */
	public void setContext(ApplicationContext context) {
		this.context = context;
	}
}
