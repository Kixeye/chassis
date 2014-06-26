package com.kixeye.chassis.transport.websocket;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on an action as a property requirement.
 * 
 * @author ebahtijaragic
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ActionPropertyRequirement {
	/**
	 * The name.
	 * 
	 * @return
	 */
	String name();
	
	/**
	 * The value.
	 * 
	 * @return
	 */
	String value();
}
