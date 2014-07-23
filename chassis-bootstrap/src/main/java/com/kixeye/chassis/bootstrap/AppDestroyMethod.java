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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * An application's destroy callback method
 *
 * @author dturner@kixeye.com
 */
public class AppDestroyMethod {
    private Method method;
    private Class<?> declaringClass;

    public AppDestroyMethod(Method method, Class<?> declaringClass) {
        this.method = method;
        this.declaringClass = declaringClass;
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new BootstrapException("@Destroy annotated methods must be static parameters.");
        }
    }

    public Void invoke() {
        try {
            method.setAccessible(true);
            method.invoke(declaringClass);
            return null;
        } catch (Exception e) {
            throw new BootstrapException("Failed to execute lifecycle method " + method, e);
        }
    }

}