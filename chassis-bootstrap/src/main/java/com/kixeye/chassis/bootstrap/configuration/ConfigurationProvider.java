package com.kixeye.chassis.bootstrap.configuration;

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

import com.kixeye.chassis.bootstrap.BootstrapException.ApplicationConfigurationNotFoundException;
import com.kixeye.chassis.bootstrap.aws.ServerInstanceContext;
import org.apache.commons.configuration.AbstractConfiguration;
import java.io.Closeable;

/**
 * Provides a strategy for resolving and/or creating an application's configuration
 *
 * @author dturner@kixeye.com
 */
public interface ConfigurationProvider extends Closeable {

    /**
     * Returns the configuration used by the application.
     * @param environment the environment the application is running in
     * @param applicationName the name of the application
     * @param applicationVersion the version of the application
     * @param serverInstanceContext information about the server that the application is running on. This is useful for providing configurations that are specific to the given server instance only.
     * @return the configuration
     * @throws ApplicationConfigurationNotFoundException
     */
    AbstractConfiguration getApplicationConfiguration(
            String environment,
            String applicationName,
            String applicationVersion,
            ServerInstanceContext serverInstanceContext) throws ApplicationConfigurationNotFoundException;

    /**
     * Write the configuration out to the source
     * @param environment the environment the application is running in
     * @param applicationName the name of the application
     * @param applicationVersion the version of the application
     * @param configuration the configuration to write
     * @param allowOverwrite whether or not to overwrite any
     */
    void writeApplicationConfiguration(String environment, String applicationName, String applicationVersion, AbstractConfiguration configuration, boolean allowOverwrite);
}
