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

//import asg.cliche.Shell;
//import asg.cliche.ShellFactory;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Process that runs an Application in an interactive shell.
 *
 * @author dturner@kixeye.com
 */
public class ExtractConfigsProcess implements Process {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractConfigsProcess.class);

    private DefaultApplication application;
    private ConfigurationWriter configurationWriter;

    public ExtractConfigsProcess(Application application, ConfigurationWriter configurationWriter) {
        Preconditions.checkState(application instanceof DefaultApplication);
        this.application = (DefaultApplication) application;
        this.configurationWriter = configurationWriter;
    }

    public void run() {
        ConfigurationBuilder configBuilder = new ConfigurationBuilder(application.getName(), application.getArguments().environment, false);
        if (application.getArguments().skipModuleScanning) {
            configBuilder.withoutModuleScanning();
        }
        if (application.getDefinition().getAppConfigPath() != null) {
            configBuilder.withApplicationProperties(application.getDefinition().getAppConfigPath());
        }
        Configuration config = configBuilder.build();
        try {
        	configurationWriter.write(config, new DefaultPropertyFilter());
        } catch (Exception e) {
            throw new BootstrapException("Failed to extract configs",e);
        }
        LOGGER.info("Configurations extracted.");
    }
}
