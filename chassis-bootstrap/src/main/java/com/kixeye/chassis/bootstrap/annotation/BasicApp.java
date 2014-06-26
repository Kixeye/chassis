package com.kixeye.chassis.bootstrap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to configure an clientApplication.
 *
 * @author dturner@kixeye.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@App
public @interface BasicApp {
    /**
     * The name of the clientApplication
     *
     * @return name
     */
    String name();

    /**
     * Spring resource location for the app's properties. Format can be anything supported by Spring's ResourceLocator
     *
     * @return propertiesResourceLocation
     */
    String propertiesResourceLocation() default "";

}
