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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.kixeye.chassis.bootstrap.AppMain.Arguments;
import com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider;
import com.kixeye.chassis.bootstrap.spring.ArchaiusSpringPropertySource;
import com.kixeye.chassis.bootstrap.spring.ArgumentsPropertySource;
import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.DynamicWatchedConfiguration;

/**
 * Represents an application configured and started by Bootstrap.
 *
 * @author dturner@kixeye.com
 */
public class Application implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private AnnotationConfigApplicationContext bootstrapApplicationContext;
    private AbstractApplicationContext applicationContext;
    private Arguments arguments;
    private AppMetadata appMetadata;
    private AbstractConfiguration configuration;
    private ConfigurationProvider configurationProvider;

    private AtomicReference<State> state = new AtomicReference<>(State.NEW);

    private enum State {
        NEW,
        STOPPED,
        STARTING,
        STOPPING,
        RUNNING
    }

    public Application(Arguments arguments) {
        Preconditions.checkNotNull(arguments);
        this.arguments = arguments;

        logger.debug("Creating Application with arguments {}", ReflectionToStringBuilder.toString(arguments));

        createApplicationContext();
    }

    /**
     * Start the application.
     */
    public Application start() {
        if (!state.compareAndSet(State.NEW, State.STARTING)) {
            if (state.get().equals(State.STOPPED)) {
                BootstrapException.applicationRestartAttempted();
            }
            return this;
        }

        logger.debug("Starting application...");

        bootstrapApplicationContext.refresh();

        logger.info("Application \"{}\" started.", appMetadata.getName());
        return this;
    }

    /**
     * Stop the application.
     */
    public void stop() {
        if (!state.compareAndSet(State.RUNNING, State.STOPPING)) {
            return;
        }
        logger.info("Stopping application \"{}\"", appMetadata.getName());
        applicationContext.close();
        bootstrapApplicationContext.close();
        invokeAppDestroyMethod();
        cleanup();
        state.compareAndSet(State.STOPPING, State.STOPPED);
    }

    private void cleanup() {
        try {
            closeConfiguration(configuration);
        } catch (IOException e) {
            throw new BootstrapException("Exception occurred while attempting to cleanup application.", e);
        }
    }

    private void closeConfiguration(org.apache.commons.configuration.Configuration configuration) throws IOException {
        if (configuration instanceof CompositeConfiguration) {
            CompositeConfiguration config = (CompositeConfiguration) configuration;
            for (int i = 0; i < config.getNumberOfConfigurations(); i++) {
                closeConfiguration(config.getConfiguration(i));
            }
        } else if (configuration instanceof AggregatedConfiguration) {
            AggregatedConfiguration config = (AggregatedConfiguration) configuration;
            for (int i = 0; i < config.getNumberOfConfigurations(); i++) {
                closeConfiguration(config.getConfiguration(i));
            }
        } else {
            if (configuration instanceof DynamicWatchedConfiguration) {
                DynamicWatchedConfiguration dynamicWatchedConfiguration = (DynamicWatchedConfiguration) configuration;
                if (dynamicWatchedConfiguration.getSource() instanceof Closeable) {
                    Closeables.close((Closeable) dynamicWatchedConfiguration.getSource(), true);
                }
            }
        }
    }

    private void invokeAppDestroyMethod() {
        AppDestroyMethod destroyMethod = appMetadata.getDestroyMethod();
        if (destroyMethod != null) {
            destroyMethod.invoke();
        }
    }

    /**
     * whether the application is running or not
     */
    public boolean isRunning() {
        return state.compareAndSet(State.RUNNING, State.RUNNING);
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
    public AbstractApplicationContext getBootstrapApplicationContext() {
        return bootstrapApplicationContext;
    }

    /**
     * getter for the child application context. the child context contains
     * beans defined by client applications
     */
    public AbstractApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.debug("Received ContextRefreshedEvent {}", event);

        if (event.getSource().equals(getBootstrapApplicationContext())) {
            //the root context is fully started
            appMetadata = bootstrapApplicationContext.getBean(AppMetadata.class);
            configuration = bootstrapApplicationContext.getBean(AbstractConfiguration.class);
            configurationProvider = bootstrapApplicationContext.getBean(ConfigurationProvider.class);

            logger.debug("Root context started");

            initClientApplication();

            return;
        }

        if (event.getSource() instanceof ApplicationContext && ((ApplicationContext) event.getSource()).getId().equals(appMetadata.getName())) {
            //the child context is fully started
            this.applicationContext = (AbstractApplicationContext) event.getSource();

            logger.debug("Child context started");
        }

        state.compareAndSet(State.STARTING, State.RUNNING);
    }

    private void initClientApplication() {
        invokeAppInitMethod();
        createChildContext();
    }

    private void invokeAppInitMethod() {
        AppInitMethod init = appMetadata.getInitMethod();
        if (init != null) {
            init.invoke(configuration, configurationProvider);
        }
    }

    private void createApplicationContext() {
        logger.debug("Creating bootstrap application context...");

        bootstrapApplicationContext = new AnnotationConfigApplicationContext();
        bootstrapApplicationContext.register(BootstrapConfiguration.class);
        bootstrapApplicationContext.getEnvironment().getPropertySources().addFirst(new ArgumentsPropertySource("bootstrap-arguments", arguments));
        bootstrapApplicationContext.addApplicationListener(this);

        logger.debug("Created bootstrap application context.");
    }

    private void createChildContext() {
        if (appMetadata.isWebapp()) {
            applicationContext = new AnnotationConfigWebApplicationContext();
            if (appMetadata.getConfigurationClasses().length > 0) {
                ((AnnotationConfigWebApplicationContext) applicationContext).register(appMetadata.getConfigurationClasses());
            }
        } else {
            applicationContext = new AnnotationConfigApplicationContext();
            if (appMetadata.getConfigurationClasses().length > 0) {
                ((AnnotationConfigApplicationContext) applicationContext).register(appMetadata.getConfigurationClasses());
            }
        }
        applicationContext.setParent(bootstrapApplicationContext);
        applicationContext.getEnvironment().getPropertySources().addFirst(new ArchaiusSpringPropertySource(appMetadata.getName() + "-archaius"));
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setEnvironment(applicationContext.getEnvironment());
        applicationContext.addBeanFactoryPostProcessor(configurer);
        applicationContext.setId(appMetadata.getName());
        applicationContext.refresh();
    }

}
