package com.kixeye.chassis.bootstrap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a static, no-arg method on an @App annotated class that is called after clientApplication shutdown.
 * This is intended to provide a mechanism to do any custom teardown logic.
 *
 * @author dturner@kixeye.com
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Destroy {
}
