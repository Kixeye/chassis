package com.kixeye.chassis.bootstrap;

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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.kixeye.chassis.bootstrap.annotation.App;
import com.kixeye.chassis.bootstrap.annotation.Destroy;
import com.kixeye.chassis.bootstrap.annotation.Init;

/**
 * Contains the values of {@link com.kixeye.chassis.bootstrap.annotation.App}. It is used
 * instead passing around the annotation instances so the code doesn't have to care which
 * annotation was the source of the properties.
 *
 * @author dturner@kixeye.com
 */
public class AppMetadata {
    private String name;
    private String propertiesResourceLocation;
    private boolean webapp;
    private Class<?>[] configurationClasses;
    private Class<?> declaringClass;
    private AppInitMethod initMethod;
    private AppDestroyMethod destroyMethod;

    public AppMetadata(String appClass, Reflections reflections) {
        initDeclaringClass(appClass, reflections);
        scanDeclaringClass();
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

    public Class<?>[] getConfigurationClasses() {
        return configurationClasses;
    }

    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public AppInitMethod getInitMethod() {
        return initMethod;
    }

    public AppDestroyMethod getDestroyMethod() {
        return destroyMethod;
    }

    private void scanDeclaringClass() {
        initAnnotationValues();
        initAppInitMethod();
        initAppDestroyMethod();
    }

    private void initAppDestroyMethod() {
        Method method = findLifecycleMethod(Destroy.class);
        if (method != null) {
            this.destroyMethod = new AppDestroyMethod(method, declaringClass);
        }
    }

    private void initAppInitMethod() {
        Method method = findLifecycleMethod(Init.class);
        if (method != null) {
            this.initMethod = new AppInitMethod(method, declaringClass);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private Method findLifecycleMethod(final Class lifecycleAnnotationClass) {
        Set<Method> methods = ReflectionUtils.getMethods(declaringClass, new Predicate<Method>() {
            @Override
            public boolean apply(Method input) {
                return input != null && input.getAnnotation(lifecycleAnnotationClass) != null;
            }
        });
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() > 1) {
            throw new BootstrapException("Found multiple " + lifecycleAnnotationClass.getSimpleName() + " methods in class " + declaringClass.getSimpleName() + ". Only 1 is allowed.");
        }
        return methods.iterator().next();
    }

    private void initAnnotationValues() {
        App app = declaringClass.getAnnotation(App.class);
        if (app != null) {
            setAnnotationValues(app.name(), app.propertiesResourceLocation(), app.webapp(), app.configurationClasses());
            return;
        }
        throw new BootstrapException("Declaring class " + declaringClass.getSimpleName() + " is not annotated with " + App.class.getSimpleName() + ".");
    }

    private void initDeclaringClass(String appClass, Reflections reflections) {
        if (loadDeclaringClass(appClass)) {
            return;
        }
        declaringClass = findDeclaringClass(App.class, reflections);
        if (declaringClass == null) {
            throw new BootstrapException("Unable to find any classes annotated with " + App.class.getSimpleName() + ".");
        }
    }

    private Class<?> findDeclaringClass(Class<? extends Annotation> annotationClass, Reflections reflections) {
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotationClass);
        if (classes.isEmpty()) {
            return null;
        }
        if (classes.size() > 1) {
            throw new BootstrapException("Found multiple classes with annotations with " + annotationClass.getSimpleName());
        }
        return classes.iterator().next();
    }


    private boolean loadDeclaringClass(String appClass) {
        if (StringUtils.isBlank(appClass)) {
            return false;
        }
        try {
            declaringClass = BootstrapConfiguration.class.getClassLoader().loadClass(appClass);
        } catch (ClassNotFoundException e) {
            throw new BootstrapException("Unable to load SpringApp annotated class " + appClass, e);
        }
        return true;
    }

    private void setAnnotationValues(String name, String propertiesResourceLocation, boolean webapp, Class<?>... configurationClasses) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));

        this.name = name;
        this.propertiesResourceLocation = StringUtils.isBlank(propertiesResourceLocation) ? null : propertiesResourceLocation;
        this.webapp = webapp;
        this.configurationClasses = configurationClasses == null ? new Class<?>[]{} : configurationClasses;
    }

}
