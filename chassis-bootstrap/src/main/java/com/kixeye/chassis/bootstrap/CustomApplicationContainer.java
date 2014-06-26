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
import java.lang.reflect.Modifier;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.kixeye.chassis.bootstrap.annotation.OnStart;
import com.kixeye.chassis.bootstrap.annotation.OnStop;
import com.kixeye.chassis.bootstrap.utils.ReflectionUtils;

/**
 * Implementation of ApplicationContainer that delegates to a client-defined class
 * to implement application behavior.
 *
 * @author dturner@kixeye.com
 */
public class CustomApplicationContainer implements ApplicationContainer {
    private ApplicationDefinition definition;
    private Function<Void, Void> onStart;
    private Function<Void, Void> onStop;
    private Object customApplicationInstance;

    public CustomApplicationContainer(ApplicationDefinition definition) {
        this.definition = definition;
        createCustomApplicationInstance(definition);
        initLifecycleMethods();
    }

    public Object getCustomApplicationInstance() {
        return customApplicationInstance;
    }

    private void createCustomApplicationInstance(ApplicationDefinition definition) {
        try {
            this.customApplicationInstance = definition.getAppClass().newInstance();
        } catch (Exception e) {
            throw new BootstrapException("Unable to create an instance of class " + definition.getAppClass());
        }
    }

    private void initLifecycleMethods() {
        onStart = createLifeCycleMethod(OnStart.class);
        onStop = createLifeCycleMethod(OnStop.class);
    }

    private Function<Void, Void> createLifeCycleMethod(Class<? extends Annotation> annotationClass) {
        final Method method = ReflectionUtils.findMethodAnnotatedWith(definition.getAppClass(), annotationClass);

        validateLifecycleMethod(method);

        return new Function<Void, Void>() {
            @Override
            public Void apply(@Nullable Void input) {
                if (method != null) {
                    method.setAccessible(true);
                    try {
                        method.invoke(customApplicationInstance);
                    } catch (Exception e) {
                        throw new BootstrapException("Failed to invoke lifecycle method " + method, e);
                    }
                }
                return null;
            }
        };
    }

    private void validateLifecycleMethod(Method method) {
        if(method.getParameterTypes().length != 0){
            throw new BootstrapException("Method " + method.getName() + " in " + definition.getAppClass() + " should have no arguments.");
        }

        if(Modifier.isStatic(method.getModifiers())){
            throw new BootstrapException("Method " + method.getName() + " in " + definition.getAppClass() + " cannot be static.");
        }

        if(!method.getReturnType().equals(Void.TYPE)){
           throw new BootstrapException("Method " + method.getName() + " in " + definition.getAppClass() + " must return void.");
        }
    }

    @Override
    public void onStart() {
        onStart.apply(null);
    }

    @Override
    public void onStop() {
        onStop.apply(null);
    }
}
