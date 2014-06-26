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

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.annotation.BasicApp;
import com.kixeye.chassis.bootstrap.annotation.SpringApp;

/**
 * Metadata describing the application as configured by the client (using @App annotations)
 *
 * @author dturner@kixeye.com
 */
public class ApplicationDefinition {
    private String appName;
    private String appConfigPath;
    private Class<?> appClass;

    public ApplicationDefinition(String appName, String appConfigPath, Class<?> appClass) {
        Preconditions.checkArgument(StringUtils.isNotBlank(appName));
        Preconditions.checkNotNull(appClass);

        this.appName = appName;
        this.appConfigPath = appConfigPath;
        this.appClass = appClass;
    }

    public static ApplicationDefinition create(Annotation annotation, Class<?> annotatedClass) {
        if (annotation instanceof SpringApp) {
            SpringApp springApp = (SpringApp) annotation;
            return new SpringApplicationDefinition(springApp.name(), springApp.propertiesResourceLocation(), annotatedClass, springApp.webapp(), springApp.configurationClasses());
        }
        if (annotation instanceof BasicApp) {
            BasicApp app = (BasicApp) annotation;
            return new ApplicationDefinition(app.name(), app.propertiesResourceLocation(), annotatedClass);
        }
        throw new BootstrapException("Unsupported Annotation " + annotation);
    }

    public String getAppName() {
        return appName;
    }

    public String getAppConfigPath() {
        return appConfigPath;
    }

    public Class<?> getAppClass() {
        return appClass;
    }
}
