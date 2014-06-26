package com.kixeye.chassis.bootstrap;

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
