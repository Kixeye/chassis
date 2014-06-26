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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.kixeye.chassis.bootstrap.annotation.App;
import com.kixeye.chassis.bootstrap.annotation.Destroy;
import com.kixeye.chassis.bootstrap.annotation.Init;
import com.kixeye.chassis.bootstrap.aws.AwsInstanceContext;
import com.kixeye.chassis.bootstrap.utils.ReflectionUtils;
import com.kixeye.chassis.bootstrap.utils.ReflectionUtils.AnnotationResult;

/**
 * Represents a client application.
 *
 * @author dturner@kixeye.com
 */
public class ClientApplication implements Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApplication.class);
    private static final String ARCHAIUS_DEPLOYMENT_ENVIRONMENT = "archaius.deployment.environment";
    private ApplicationContainerFactory applicationContainerFactory;
    private AbstractConfiguration configuration;
    private ApplicationContainer applicationContainer;
    private boolean running = false;
    private String environment;
    private boolean skipModuleScanning;
    private ApplicationDefinition definition;
    private String setAnnotatedClassName;
    private CuratorFrameworkBuilder curatorFrameworkBuilder;
    private AwsInstanceContext awsInstanceContext;

    private AppInitMethod initMethod;
    private AppDestroyMethod destroyMethod;

    public ClientApplication(
            String environment,
            String zookeeperHost,
            int exhibitorPort,
            String [] exhibitorHosts,
            String annotatedClassName,
            boolean skipModuleScanning,
            ApplicationContainerFactory applicationContainerFactory,
            AwsInstanceContext awsInstanceContext) {

        setAwsInstanceContext(awsInstanceContext);
        setEnvironment(environment);
        setSetAnnotatedClassName(annotatedClassName);
        setSkipModuleScanning(skipModuleScanning);
        setApplicationContainerFactory(applicationContainerFactory);

        initEnvironmentForArchaiusHack(environment);

        initAppMetadata();
        initConfiguration(zookeeperHost, exhibitorPort, exhibitorHosts);
        initContainerAdapter();
        setupLifecycleMethods();

        tagInstance( configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_NAME.getPropertyName()) );
    }

    private void tagInstance( String tagInstanceName ) {
        if(awsInstanceContext != null){
            awsInstanceContext.setAppName(getName());
            awsInstanceContext.setVersion(configuration.getString(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName()));
            awsInstanceContext.tagInstance(tagInstanceName);
        }
    }

    private void setAwsInstanceContext(AwsInstanceContext awsInstanceContext) {
        this.awsInstanceContext = awsInstanceContext;
    }

    private void initEnvironmentForArchaiusHack(String environment) {
        Properties systemProps = System.getProperties();
        if (systemProps.getProperty(ARCHAIUS_DEPLOYMENT_ENVIRONMENT) == null) {
            systemProps.setProperty(ARCHAIUS_DEPLOYMENT_ENVIRONMENT, environment);
        }
    }

    private void setApplicationContainerFactory(ApplicationContainerFactory applicationContainerFactory) {
        Preconditions.checkNotNull(applicationContainerFactory);
        this.applicationContainerFactory = applicationContainerFactory;
    }

    private void setSkipModuleScanning(boolean skipModuleScanning) {
        this.skipModuleScanning = skipModuleScanning;
    }

    private void setSetAnnotatedClassName(String setAnnotatedClassName) {
        this.setAnnotatedClassName = setAnnotatedClassName;
    }

    private void setEnvironment(String environment) {
        if(StringUtils.isBlank(environment)){
            throw new BootstrapException("Environment is required but not found.");
        }
        //default to the given environment to allow for override
        this.environment = environment;
    }

    private void initContainerAdapter() {
        this.applicationContainer = applicationContainerFactory.getContainerAdapter(definition);
    }

    private void initConfiguration(String zookeeperHost, int exhibitorPort, String [] exhibitorHosts) {
        ConfigurationBuilder builder = new ConfigurationBuilder(definition.getAppName(), environment, true);
        String appVersion = definition.getAppClass().getPackage().getImplementationVersion();
        if (StringUtils.isNotBlank(appVersion)) {
            builder.withAppVersion(appVersion);
        }
        if (zookeeperHost != null) {
            initCuratorFrameworkBuilderIfNecessary();
            builder.withZookeeper(zookeeperHost);
            curatorFrameworkBuilder.withZookeeper(zookeeperHost);
        }
        if(exhibitorHosts != null){
            initCuratorFrameworkBuilderIfNecessary();
            builder.withExhibitors(exhibitorPort, exhibitorHosts);
            curatorFrameworkBuilder.withExhibitors(exhibitorPort, exhibitorHosts);
        }
        if (StringUtils.isNotBlank(definition.getAppConfigPath())) {
            builder.withApplicationProperties(definition.getAppConfigPath());
        }
        if (skipModuleScanning) {
            builder.withoutModuleScanning();
        }
        if(awsInstanceContext != null){
            builder.withAwsInstanceContext(awsInstanceContext);
        }
        this.configuration = builder.build();
    }

    private void initCuratorFrameworkBuilderIfNecessary() {
        if(this.curatorFrameworkBuilder == null){
            this.curatorFrameworkBuilder = new CuratorFrameworkBuilder(false);
        }
    }

    private void initAppMetadata() {
        if (StringUtils.isBlank(setAnnotatedClassName)) {
            scanForMetadata();
            return;
        }
        manuallyLoadMetadata();
    }

    private void manuallyLoadMetadata() {
        Class<?> annotatedClass;
        try {
            annotatedClass = AppMain.class.getClassLoader().loadClass(setAnnotatedClassName);
        } catch (ClassNotFoundException e) {
            throw new BootstrapException("Failed to load setAnnotatedClassName " + setAnnotatedClassName
                    , e);
        }
        AnnotationResult<Annotation> result = ReflectionUtils.findAppInClass(annotatedClass);
        this.definition = ApplicationDefinition.create(result.getAnnotation(), result.getType());
    }

    private void scanForMetadata() {
        AnnotationResult<Annotation> result = ReflectionUtils.findApp();
        if (result == null) {
            throw new BootstrapException("Found no classes in the classpath annotated with @" + App.class.getSimpleName());
        }
        this.definition = ApplicationDefinition.create(result.getAnnotation(), result.getType());
    }

    public void start() {
        if(initMethod != null){
            initMethod.call();
        }
        getLogger().info("Starting clientApplication " + getName());
        applicationContainer.onStart();
        running = true;
        LOGGER.info("App \"" + getName() + "\" is running...");
    }

    public void stop() {
        getLogger().info("Shutting down clientApplication " + getName());
        applicationContainer.onStop();
        if(destroyMethod != null){
            destroyMethod.call();
        }
        running = false;
    }

    public String getName() {
        return definition.getAppName();
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public ApplicationDefinition getDefinition() {
        return definition;
    }

    @Override
    public ApplicationContainer getApplicationContainer() {
        return applicationContainer;
    }

    private void setupLifecycleMethods() {
        Method init = findLifecycleMethod(Init.class);
        if (init != null) {
            this.initMethod = new AppInitMethod(init);
        }
        Method destroy = findLifecycleMethod(Destroy.class);
        if (destroy != null) {
            this.destroyMethod = new AppDestroyMethod(destroy);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Method findLifecycleMethod(final Class lifecycleAnnotationClass) {
        Set<Method> methods = org.reflections.ReflectionUtils.getMethods(definition.getAppClass(), new Predicate<Method>() {
            @Override
            public boolean apply(@Nullable Method input) {
                return input != null && input.getAnnotation(lifecycleAnnotationClass) != null;
            }
        });
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() > 1) {
            throw new BootstrapException("Found multiple " + lifecycleAnnotationClass.getSimpleName() + " methods in class " + definition.getAppClass().getSimpleName() + ". Only 1 is allowed.");
        }
        return methods.iterator().next();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(ClientApplication.class);
    }

    private class AppInitMethod {
        private Method method;

        protected AppInitMethod(Method method) {
            this.method = method;
            validate();
        }

        public Void call() {
            try {
                method.setAccessible(true);
                List<Object> args = new ArrayList<>();
                args.add(configuration);
                if(method.getParameterTypes().length==2){
                    if(curatorFrameworkBuilder != null){
                        args.add(curatorFrameworkBuilder.build());
                    }else{
                        args.add(null);
                    }
                }
                method.invoke(definition.getAppClass(), args.toArray());
                return null;
            } catch (Exception e) {
                throw new BootstrapException("Failed to execute lifecycle method " + method, e);
            }
        }

        private void validate() {
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new BootstrapException("@" + definition.getAppClass().getSimpleName() + " annotated methods must be static parameters.");
            }
            if (method.getParameterTypes().length < 1 || method.getParameterTypes().length > 2) {
                throw new BootstrapException("@" + definition.getAppClass().getSimpleName() + " annotated methods must have a signature like: void static method(org.apache.commons.configuration.Configuration) or void static method(org.apache.commons.configuration.Configuration, com.netflix.curator.framework.CuratorFramework).");
            }
            if (!Configuration.class.isAssignableFrom(method.getParameterTypes()[0])) {
                throw new BootstrapException("@" + definition.getAppClass().getSimpleName() + " annotated methods must have a signature like: void static method(org.apache.commons.configuration.Configuration) or void static method(org.apache.commons.configuration.Configuration, com.netflix.curator.framework.CuratorFramework).");
            }
            if(method.getParameterTypes().length == 2 && !CuratorFramework.class.isAssignableFrom(method.getParameterTypes()[1])){
                throw new BootstrapException("@" + definition.getAppClass().getSimpleName() + " annotated methods must have a signature like: void static method(org.apache.commons.configuration.Configuration) or void static method(org.apache.commons.configuration.Configuration, com.netflix.curator.framework.CuratorFramework).");
            }
        }

    }

    private class AppDestroyMethod {
        private Method method;

        protected AppDestroyMethod(Method method) {
            this.method = method;
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new BootstrapException("@" + definition.getAppClass().getSimpleName() + " annotated methods must be static parameters.");
            }
        }

        public Void call() {
            try {
                method.setAccessible(true);
                method.invoke(definition.getAppClass());
                return null;
            } catch (Exception e) {
                throw new BootstrapException("Failed to execute lifecycle method " + method, e);
            }
        }

    }
}
