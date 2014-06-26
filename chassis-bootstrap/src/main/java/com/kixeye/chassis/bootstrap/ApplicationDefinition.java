package com.kixeye.chassis.bootstrap;

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
