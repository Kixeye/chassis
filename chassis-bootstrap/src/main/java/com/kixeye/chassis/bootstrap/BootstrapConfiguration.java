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

import com.kixeye.chassis.bootstrap.aws.ServerInstanceContext;
import com.kixeye.chassis.bootstrap.configuration.BootstrapConfigKeys;
import com.kixeye.chassis.bootstrap.configuration.ConfigurationBuilder;
import com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider;
import com.kixeye.chassis.bootstrap.configuration.zookeeper.ZookeeperConfigurationProvider;

import org.apache.commons.configuration.AbstractConfiguration;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Spring configuration for beans exposed the Bootstrap spring context. The bootstrap
 * spring context is the root application context, and any spring beans introduced by
 * applications are introduced in a child spring context.
 *
 * @author dturner@kixeye.com
 */
@Configuration
public class BootstrapConfiguration {

    public static final Reflections REFLECTIONS = new Reflections("", "com.kixeye");

    @Value("${appEnvironment:null}")
    private String appEnvironment;

    @Value("${zookeeperConnectionString:null}")
    private String zookeeperConnectionString;

    @Value("${appClass:null}")
    private String appClass;

    @Value("${exhibitorPort:8080}")
    private int exhibitorPort;

    @Value("${exhibitorHosts:null}")
    private String[] exhibitorHosts;

    @Value("${addSystemConfigs:true}")
    private boolean addSystemConfigs;

    @Value("${scanModuleConfigurations:true}")
    private boolean scanModuleConfigurations;

    @Value("${skipServerInstanceContextInitialization:true}")
    private boolean skipServerInstanceContextInitialization;

    @Bean
    public Reflections reflections() {
        return REFLECTIONS;
    }

    @Bean
    public AppMetadata appMetadata() {
        return new AppMetadata(appClass, reflections());
    }

	@Bean
    @SuppressWarnings("resource")
    public AbstractConfiguration applicationConfiguration() throws ClassNotFoundException {
        AppMetadata appMetadata = appMetadata();
        ServerInstanceContext serverInstanceContext = serverInstanceContext();
        if(appEnvironment == null && serverInstanceContext != null){
            appEnvironment = serverInstanceContext.getEnvironment();
        }
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder(appMetadata.getName(), appEnvironment, addSystemConfigs, reflections());
        configurationBuilder.withConfigurationProvider(configurationProvider());
        configurationBuilder.withServerInstanceContext(serverInstanceContext());
        configurationBuilder.withApplicationProperties(appMetadata.getPropertiesResourceLocation());
        configurationBuilder.withScanModuleConfigurations(scanModuleConfigurations);
        configurationBuilder.withAppVersion(appMetadata.getDeclaringClass().getPackage().getImplementationVersion());
        AbstractConfiguration configuration = configurationBuilder.build();
        if(serverInstanceContext != null){
            serverInstanceContext.setAppName(appMetadata.getName());
            serverInstanceContext.setVersion(configuration.getString(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName()));
            serverInstanceContext.tagInstance();
        }
        return configuration;
    }

    @Bean
    public ServerInstanceContext serverInstanceContext() {
        if (skipServerInstanceContextInitialization) {
            return null;
        }
        return ServerInstanceContext.initialize();
    }

    @Bean(destroyMethod = "close")
    public ConfigurationProvider configurationProvider() {
        if (zookeeperConnectionString != null) {
            return new ZookeeperConfigurationProvider(zookeeperConnectionString);
        }
        if (exhibitorHosts != null && exhibitorHosts.length > 0) {
            return new ZookeeperConfigurationProvider(exhibitorPort, exhibitorHosts);
        }
        if (serverInstanceContext() != null && serverInstanceContext().getExhibitorHost() != null) {
            return new ZookeeperConfigurationProvider(serverInstanceContext().getExhibitorPort(), serverInstanceContext().getExhibitorHost());
        }
        return null;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceHolderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setNullValue("null");
        return propertySourcesPlaceholderConfigurer;
    }

}
