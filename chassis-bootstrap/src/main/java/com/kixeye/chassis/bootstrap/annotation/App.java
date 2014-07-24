package com.kixeye.chassis.bootstrap.annotation;

/*
 * #%L
 * Chassis Bootstrap
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
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
public @interface App{
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
     */
    public Class<?>[] configurationClasses() default {};

    /**
     * whether or not it is a web application
     * @return
     */
    public boolean webapp() default true;

}
