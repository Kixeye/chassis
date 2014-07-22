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

import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.BootstrapConfiguration;
import com.kixeye.chassis.bootstrap.BootstrapException;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Contains the values of {@link App} and the deprecated {@link SpringApp}. It is used
 * instead passing around the annotation instances so the code doesn't have to care which
 * annotation was the source of the properties.
 *
 * @author dturner@kixeye.com
 */
public class AppMetadata {
    private String name;
    private String propertiesResourceLocation;
    private boolean webapp;
    private Class[] configurationClasses;

    public AppMetadata(String name, String propertiesResourceLocation, boolean webapp, Class<?>... configurationClasses) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));

        this.name = name;
        this.propertiesResourceLocation = propertiesResourceLocation;
        this.webapp = webapp;
        this.configurationClasses = configurationClasses == null ? new Class<?>[]{} : configurationClasses;
    }

    public String getName() {
        return name;
    }

    public String getPropertiesResourceLocation() {
        return propertiesResourceLocation;
    }

    public boolean isWebapp() {
        return webapp;
    }

    public Class[] getConfigurationClasses() {
        return configurationClasses;
    }

    public static AppMetadata create(String appClass, Reflections reflections) {
        AppMetadata appMetadata = null;

        if (appClass == null) {
            appMetadata = scanForAppMetadata(reflections);
        } else {
            appMetadata = loadAppMetadata(appClass);
        }

        if (appMetadata == null) {
            throw new BootstrapException("Found no classes annotated with " + App.class.getSimpleName() + " or " + SpringApp.class.getSimpleName());
        }

        return appMetadata;
    }

    private static AppMetadata loadAppMetadata(String appClass) {
        App app = loadApp(App.class, appClass);
        SpringApp springApp = loadApp(SpringApp.class, appClass);

        if (app != null && springApp != null) {
            throw new BootstrapException("Found both " + App.class.getSimpleName() + " and " + SpringApp.class.getSimpleName() + " on configuration class " + appClass + ". Only one is allowed (and " + App.class.getSimpleName() + " is preferred).");
        }

        if (app != null) {
            return new AppMetadata(app.name(), app.propertiesResourceLocation(), app.webapp(), app.configurationClasses());
        }

        if (springApp != null) {
            return new AppMetadata(springApp.name(), springApp.propertiesResourceLocation(), springApp.webapp(), springApp.configurationClasses());
        }

        return null;
    }

    private static AppMetadata scanForAppMetadata(Reflections reflections) {
        App app = scanForApp(App.class, reflections);
        SpringApp springApp = scanForApp(SpringApp.class, reflections);

        if (app != null && springApp != null) {
            throw new BootstrapException("Found classes annotated with both " + App.class.getSimpleName() + " and " + SpringApp.class.getSimpleName() + ". Only one is allowed (and " + App.class.getSimpleName() + " is preferred).");
        }

        if (app != null) {
            return new AppMetadata(app.name(), app.propertiesResourceLocation(), app.webapp(), app.configurationClasses());
        }

        if (springApp != null) {
            return new AppMetadata(springApp.name(), springApp.propertiesResourceLocation(), springApp.webapp(), springApp.configurationClasses());
        }

        return null;
    }

    private static <T extends Annotation> T loadApp(Class<T> annotationClass, String appClass) {
        try {
            Class<?> annotatedClass = annotatedClass = BootstrapConfiguration.class.getClassLoader().loadClass(appClass);
            return annotatedClass.getAnnotation(annotationClass);
        } catch (ClassNotFoundException e) {
            throw new BootstrapException("Unable to load SpringApp annotated class " + appClass, e);
        }
    }

    private static <T extends Annotation> T scanForApp(Class<T> annotationClass, Reflections reflections) {
        Set<Class<?>> springAppClasses = reflections.getTypesAnnotatedWith(annotationClass);
        if (springAppClasses.isEmpty()) {
            return null;
        }
        if (springAppClasses.size() > 1) {
            throw new BootstrapException("Found multiple classes with annotations with " + annotationClass.getSimpleName());
        }
        return springAppClasses.iterator().next().getAnnotation(annotationClass);
    }
}
