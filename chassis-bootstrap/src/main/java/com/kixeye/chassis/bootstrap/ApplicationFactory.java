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
