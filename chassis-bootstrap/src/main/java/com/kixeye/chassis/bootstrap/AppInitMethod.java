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

import com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider;
import org.apache.commons.configuration.Configuration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * An application's init callback method
 *
 * @author dturner@kixeye.com
 */
public class AppInitMethod {
    private Class<?> declaringClass;
    private Method method;

    public AppInitMethod(Method method, Class<?> declaringClass) {
        this.method = method;
        this.declaringClass = declaringClass;
        validate();
    }

    public Void invoke(Configuration configuration, ConfigurationProvider configurationProvider) {
        try {
            method.setAccessible(true);
            List<Object> args = new ArrayList<>();
            args.add(configuration);
            if (method.getParameterTypes().length == 2) {
                if (configurationProvider != null) {
                    args.add(configurationProvider);
                } else {
                    args.add(null);
                }
            }
            method.invoke(declaringClass, args.toArray());
            return null;
        } catch (Exception e) {
            throw new BootstrapException("Failed to execute lifecycle method " + method, e);
        }
    }

    private void validate() {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new BootstrapException("@Init annotated methods must be static parameters.");
        }
        if (method.getParameterTypes().length < 1 || method.getParameterTypes().length > 2) {
            throw new BootstrapException("@Init annotated methods must have a signature like: void static method(org.apache.commons.configuration.Configuration) or void static method(org.apache.commons.configuration.Configuration, com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider).");
        }
        if (!Configuration.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw new BootstrapException("@Init annotated methods must have a signature like: void static method(org.apache.commons.configuration.Configuration) or void static method(org.apache.commons.configuration.Configuration, com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider).");
        }
        if (method.getParameterTypes().length == 2 && !ConfigurationProvider.class.isAssignableFrom(method.getParameterTypes()[1])) {
            throw new BootstrapException("@Init annotated methods must have a signature like: void static method(org.apache.commons.configuration.Configuration) or void static method(org.apache.commons.configuration.Configuration, com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider).");
        }
    }

}