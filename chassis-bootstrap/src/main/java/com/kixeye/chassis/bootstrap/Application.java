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
import com.kixeye.chassis.bootstrap.AppMain.Arguments;
import com.kixeye.chassis.bootstrap.annotation.AppMetadata;
import com.kixeye.chassis.bootstrap.annotation.SpringApp;
import com.kixeye.chassis.bootstrap.spring.ArgumentsPropertySource;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Represents an application configured and started by Bootstrap.
 *
 * @author dturner@kixeye.com
 */
public class Application implements ApplicationListener<ContextRefreshedEvent>{

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private AnnotationConfigApplicationContext applicationContext;
    private AbstractApplicationContext childApplicationContext;
    private Arguments arguments;
    private AppMetadata appMetadata;

    public Application(Arguments arguments) {
        Preconditions.checkNotNull(arguments);
        this.arguments = arguments;

        logger.debug("Creating Application with arguments {}", ReflectionToStringBuilder.toString(arguments));

        createApplicationContext();
    }

    /**
     * Start the application.
     * @throws InterruptedException
     */
    public Application start() throws InterruptedException {
        logger.debug("Starting application...");

        applicationContext.refresh();

        logger.info("Application \"{}\" started.", appMetadata.getName());
        return this;
    }

    /**
     * Stop the application.
     */
    public void stop() {
        if (!applicationContext.isRunning()) {
            return;
        }
        logger.info("Stopping application \"{}\"", appMetadata.getName());
        applicationContext.stop();
    }

    /**
     * whether the application is running or not
     */
    public boolean isRunning() {
        return applicationContext.isRunning() && childApplicationContext != null && childApplicationContext.isRunning();
    }

    /**
     * getter for arguments
     */
    public Arguments getArguments() {
        return arguments;
    }

    /**
     * getter for the root application context. the root context contains
     * beans defined by the bootstrap library itself.
     */
    public AbstractApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * getter for the child application context. the child context contains
     * beans defined by client applications
     */
    public AbstractApplicationContext getChildApplicationContext() {
        return childApplicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.debug("Received ContextRefreshedEvent {}", event);

        if(event.getSource().equals(getApplicationContext())){
            //the root context is fully started
            appMetadata = applicationContext.getBean(AppMetadata.class);

            logger.debug("Root context started");
            return;
        }

        if(event.getSource() instanceof ApplicationContext && ((ApplicationContext) event.getSource()).getId().equals(appMetadata.getName())){
            //the child context is fully started
            this.childApplicationContext = (AbstractApplicationContext) event.getSource();

            logger.debug("Child context started");
        }

    }

    private void createApplicationContext() {
        logger.debug("Creating bootstrap application context...");

        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(BootstrapConfiguration.class);
        applicationContext.getEnvironment().getPropertySources().addFirst(new ArgumentsPropertySource("bootstrap-arguments", arguments));
        applicationContext.addApplicationListener(this);

        logger.debug("Created bootstrap application context.");
    }
}
