package com.kixeye.chassis.transport.websocket;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.Mapping;

/**
 * Maps a method as an action.
 * 
 * @author ebahtijaragic
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface ActionMapping {
	/**
	 * A list of actions this method can perform.
	 * 
	 * @return
	 */
	String[] value();
	
	/**
	 * A list of property requirements for executing this mapping.
	 * 
	 * @return
	 */
	ActionPropertyRequirement[] propertyRequirements() default {};
}
