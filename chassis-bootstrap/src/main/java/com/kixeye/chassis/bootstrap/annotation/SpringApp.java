package com.kixeye.chassis.bootstrap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to configure an application.
 *
 * @author dturner@kixeye.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@App
public @interface SpringApp {
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

    /**
     * Spring Java config classes. Should be annotated with @Configuration
     *
     * @return
     */
    public Class<?>[] configurationClasses();

    /**
     * whether or not it is a web application
     * @return
     */
    public boolean webapp() default true;

}
