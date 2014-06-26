package com.kixeye.chassis.bootstrap;

import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.aws.AwsInstanceContext;

/**
 * Creates an Application bases an arguments from the client starting from
 * the Main class.
 *
 * @author dturner@kixeye.com
 */
public class ApplicationFactory {
    private ApplicationContainerFactory applicationContainerFactory;

    public ApplicationFactory(ApplicationContainerFactory applicationContainerFactory){
        Preconditions.checkNotNull(applicationContainerFactory);
        this.applicationContainerFactory = applicationContainerFactory;
    }

    public Application getApplication(AppMain.Arguments arguments, AwsInstanceContext awsInstanceContext){
        if(arguments.extractConfigs){
            return new DefaultApplication(arguments);
        }
        return new ClientApplication(
                arguments.environment,
                arguments.getZookeeperHost(),
                arguments.exhibitorPort,
                arguments.getExhibitorHosts(),
                arguments.appClass,
                arguments.skipModuleScanning,
                applicationContainerFactory,
                awsInstanceContext);
    }
}
