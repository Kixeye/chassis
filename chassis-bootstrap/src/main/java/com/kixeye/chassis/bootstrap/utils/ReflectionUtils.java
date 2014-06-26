package com.kixeye.chassis.bootstrap.utils;

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

import com.google.common.base.Predicate;
import com.kixeye.chassis.bootstrap.BootstrapException;
import com.kixeye.chassis.bootstrap.annotation.App;

import org.reflections.Reflections;

import javax.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * bootstrap specific reflection helpers
 *
 * @author dturner@kixeye.com
 */
public class ReflectionUtils {

    private static Reflections REFLECTIONS = new Reflections("com.kixeye");

    /**
     * Searches everything in com.kixeye.* for classes with a Type annotation that
     *
     * @return
     */
	public static AnnotationResult<Annotation> findApp() {
        return findApp(REFLECTIONS);
    }

    public static AnnotationResult<Annotation> findApp(Reflections reflections) {
        Set<Class<?>> annotationTypes = reflections.getTypesAnnotatedWith(App.class, true);
        if (annotationTypes.size() == 0) {
            return null;
        }
        return findApp(reflections, annotationTypes);
    }

    public static AnnotationResult<Annotation> findApp(Reflections reflections, Class<?>...annotationTypes) {
        return findApp(reflections, new HashSet<Class<?>>(Arrays.asList(annotationTypes)));
    }

    @SuppressWarnings("unchecked")
	public static AnnotationResult<Annotation> findApp(Reflections reflections, Set<Class<?>> annotationTypes) {
        Set<AnnotationResult<Annotation>> results = new LinkedHashSet<>();
        for (Class<?> annotationType : annotationTypes) {
            Set<Class<?>> types = reflections.getTypesAnnotatedWith((Class<Annotation>) annotationType);
            if (types.isEmpty()) {
                continue;
            }
            if (types.size() > 1) {
                throw new BootstrapException("Found multiple types annotated with " + annotationType.getName());
            }
            Class<?> type = types.iterator().next();
            results.add(new AnnotationResult<>(type, type.getAnnotation((Class<Annotation>) annotationType)));
        }

        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new BootstrapException("Found multiple classes with annotations annotated by @" + App.class.getSimpleName());
        }

        return results.iterator().next();
    }

    public static AnnotationResult<Annotation> findAppInClass(Class<?> annotatedClass) {
        Annotation[] annotations = annotatedClass.getAnnotations();
        if (annotations.length == 0) {
            return null;
        }
        Set<Annotation> found = new LinkedHashSet<>();
        for (Annotation annotation : annotations) {
            Annotation [] declaredAnnotations = annotation.annotationType().getDeclaredAnnotations();
            for(Annotation declaredAnnotation:declaredAnnotations){
                if(declaredAnnotation.annotationType() == App.class){
                    found.add(annotation);
                }
            }
        }
        if (found.size() == 0) {
            return null;
        }
        if (found.size() > 1) {
            throw new BootstrapException("Found multiple @" + App.class.getSimpleName() + " annotations in class " + annotatedClass);
        }
        return new AnnotationResult<>(annotatedClass, found.iterator().next());
    }

    @SuppressWarnings("unchecked")
	public static Method findMethodAnnotatedWith(Class<?> appClass, final Class<? extends Annotation> annotationClass) {
        Set<Method> methods = org.reflections.ReflectionUtils.getMethods(appClass, new Predicate<Method>() {

            @Override
            public boolean apply(@Nullable Method input) {
                return (input != null) && (input.getAnnotation(annotationClass) != null);
            }
        });

        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() > 1) {
            throw new BootstrapException("Found multiple methods on class " + appClass + " annotated with @" + annotationClass.getSimpleName() + ". Expected 1.");
        }
        return methods.iterator().next();
    }

    /**
     * Result for an annotation search which contains the type and
     * Annotation found.
     *
     * @param <T>
     */
    public static class AnnotationResult<T extends Annotation> {
        private T annotation;
        private Class<?> type;

        public AnnotationResult(Class<?> type, T annotation) {
            this.type = type;
            this.annotation = annotation;
        }

        public T getAnnotation() {
            return annotation;
        }

        public Class<?> getType() {
            return type;
        }
    }
}
