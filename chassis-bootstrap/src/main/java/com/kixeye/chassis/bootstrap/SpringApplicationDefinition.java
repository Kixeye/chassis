package com.kixeye.chassis.bootstrap;

import com.google.common.base.Preconditions;

/**
 * An ApplicationDefinition which include Spring related configurations
 *
 * @author dturner@kixeye.com
 */
public class SpringApplicationDefinition extends ApplicationDefinition {
    private Class<?> [] configClasses;
    private boolean webapp;

    public SpringApplicationDefinition(String appName, String appConfigPath, Class<?> appClass, boolean webapp, Class<?>...configClasses) {
        super(appName, appConfigPath, appClass);
        Preconditions.checkNotNull(configClasses);

        this.webapp = webapp;
        this.configClasses = configClasses;

    }

    public Class<?>[] getConfigurationClasses() {
        return configClasses;
    }

    public boolean isWebapp() {
        return webapp;
    }
}
