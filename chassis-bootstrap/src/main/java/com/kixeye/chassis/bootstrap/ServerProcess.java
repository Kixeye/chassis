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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the Application process as a SERVER (a blocking process)
 *
 * @author dturner@kixeye.com
 */
public class ServerProcess implements Process{
    private static Logger logger;

    private Application application;

    public ServerProcess(Application application){
        this.application = application;
        logger = LoggerFactory.getLogger(application.getName());
    }

    @Override
    public void run() {
        registerShutdownHook();
        application.start();
        while (application.isRunning()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new BootstrapException("Unexpected main thread interruption", e);
            }
        }
    }

    private void registerShutdownHook() {
        final Application thisApp = this.application;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                logger.debug("Shutdown detected. Shutting down " + thisApp.getName());
                thisApp.stop();
            }
        }));
    }
}
