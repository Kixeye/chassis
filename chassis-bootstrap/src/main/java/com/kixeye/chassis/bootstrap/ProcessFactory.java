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

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;

import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.AppMain.Arguments;

/**
 * Responsible for constructing the appropriate Process
 *
 * @author dturner@kixeye.com
 */
public class ProcessFactory {
    public Process getProcess(Application application, Arguments arguments) {
        Preconditions.checkNotNull(application);
        Preconditions.checkNotNull(arguments);

        if (arguments.extractConfigs) {
            return createExtractConfigsProcess(application, arguments);
        }
        return new ServerProcess(application);
    }

    private Process createExtractConfigsProcess(Application application, Arguments arguments) {
        if (arguments.extractConfigFile != null && containsZookeeperArguments(arguments)) {
            throw new BootstrapException("Arguments cannot contain both zookeeper arguments and configuration extract file.");
        }
        if (arguments.extractConfigFile != null) {
            return new ExtractConfigsProcess(application, new PropertiesFileConfigurationWriter(arguments.extractConfigFile));
        }
        String version = System.getProperty(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName());
        if (StringUtils.isBlank(version)) {
            throw new BootstrapException("Application version system property \"" + BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName() + "\" is required when publishing default configs to Zookeeper. ");
        }
        return new ExtractConfigsProcess(
                application,
                new ZookeeperConfigurationWriter(application.getName(),
                        arguments.environment,
                        version,
                        createCuratorFramework(arguments),
                        arguments.forceExtractConfigs));
    }

    private CuratorFramework createCuratorFramework(Arguments arguments) {
        CuratorFrameworkBuilder builder = new CuratorFrameworkBuilder(true);
        if (arguments.getZookeeperHost() != null) {
            return builder.withZookeeper(arguments.getZookeeperHost()).build();
        }
        return builder.withExhibitors(arguments.exhibitorPort, arguments.getExhibitorHosts()).build();
    }

    private boolean containsZookeeperArguments(Arguments arguments) {
        return arguments.getZookeeperHost() != null || arguments.getExhibitorHosts() != null;
    }
}
