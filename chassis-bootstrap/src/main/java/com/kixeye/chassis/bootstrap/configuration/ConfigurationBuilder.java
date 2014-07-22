package com.kixeye.chassis.bootstrap.configuration;

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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.BootstrapException;
import com.kixeye.chassis.bootstrap.BootstrapException.ApplicationConfigurationNotFoundException;
import com.kixeye.chassis.bootstrap.aws.ServerInstanceContext;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.SystemPropertyUtils;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builder class for creating the client application's configuration.  Configurations are hierarchical, and property lookup
 * evaluation will take place in the following order:
 * <p/>
 * <p/>
 * Apps using Zookeeper for config management:
 * system properties --> zookeeper properties --> client application properties (from @App.propertiesResourceLocation())
 * <p/>
 * Apps NOT using Zookeeper for config management:
 * system properties --> client application properties --> dependency module default properties
 * <p/>
 * The following exceptions to this are:
 * <p/>
 * <li>client application version can be defined in system properties or client application properties (@see APP_VERSION_KEY), and will be evaluated before zookeeper properties</li>
 * <li>zookeeper connection defaults can be in system properties or client application properties, and will be evaluated before default zookeeper properties</li>
 *
 * @author dturner@kixeye.com
 */
public class ConfigurationBuilder implements Closeable {
    public static final String LOCAL_INSTANCE_ID = "local";
    public static final String UNKNOWN = "unknown";
    private static final String ARCHAIUS_DEPLOYMENT_ENVIRONMENT = "archaius.deployment.environment";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationBuilder.class);
    private static final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
    private final Reflections reflections;

    private boolean publishDefaults = false;
    private boolean scanModuleConfigurations = true;
    private boolean addSystemConfigs = true;
    private String applicationPropertiesPath;
    private String appName;
    private String appEnvironment;
    private String appVersion;
    private ServerInstanceContext serverInstanceContext;

    //properties from local client application file(s)
    private AbstractConfiguration applicationFileConfiguration;

    //instance properties loaded from configuration provider
    private AbstractConfiguration applicationConfiguration;

    //properties loaded from dependency modules
    private AbstractConfiguration moduleDefaultConfiguration;

    private ConfigurationProvider configurationProvider;
    private boolean configureArchaius = true;

    public ConfigurationBuilder(String appName, String appEnvironment, boolean addSystemConfigs, Reflections reflections) {
        Preconditions.checkArgument(StringUtils.isNotBlank(appName));
        Preconditions.checkArgument(StringUtils.isNotBlank(appEnvironment));
        Preconditions.checkNotNull(reflections);

        this.appName = appName;
        this.appEnvironment = appEnvironment;
        this.addSystemConfigs = addSystemConfigs;
        this.reflections = reflections;

        System.setProperty(BootstrapConfigKeys.APP_NAME_KEY.getPropertyName(), appName);
        System.setProperty(BootstrapConfigKeys.APP_ENVIRONMENT_KEY.getPropertyName(), appEnvironment);
    }

    //add properties to the base PropertiesConfiguration. If the base already contains a key that
    //is to be added, an exception is thrown.
    private static void join(Map<String, Object> base, Properties properties,
                             String propertyFile, String[] propertyFiles) {
        for (Object key : properties.keySet()) {
            if (base.get(key) != null) {
                BootstrapException.moduleKeysConflictFound(propertyFile, propertyFiles);
            }
            base.put((String) key, properties.get(key));
        }
    }

    /**
     * Build the Configuration
     *
     * @return the configuration
     */
    public AbstractConfiguration build() {
        initApplicationFileConfiguration();
        initAppVersion();
        initApplicationConfiguration();
        initModuleConfiguration();

        ConcurrentCompositeConfiguration finalConfiguration = new ConcurrentCompositeConfiguration();
        if (addSystemConfigs) {
            finalConfiguration.addConfiguration(new ConcurrentMapConfiguration(new SystemConfiguration()));
        }

        finalConfiguration.addProperty(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName(), appVersion);

        addServerInstanceProperties(finalConfiguration);

        if (applicationConfiguration == null) {
            LOGGER.warn("\n\n    ****** Default configuration being used ******\n    client application \"" + appName + "\" is being configured with modules defaults. Defaults should only be used in development environments.\n    In non-developement environments, a configuration provider should be used to configure the client application and it should define ALL required configuration properties.\n");
            finalConfiguration.addConfiguration(applicationFileConfiguration);
            finalConfiguration.addConfiguration(moduleDefaultConfiguration);
        } else {
            finalConfiguration.addConfiguration(applicationConfiguration);
            finalConfiguration.addConfiguration(applicationFileConfiguration);
        }

        configureArchaius(finalConfiguration);

        logConfiguration(finalConfiguration);

        return finalConfiguration;
    }

    private void configureArchaius(ConcurrentCompositeConfiguration finalConfiguration) {
        if (configureArchaius) {
            Properties systemProps = System.getProperties();
            if (systemProps.getProperty(ARCHAIUS_DEPLOYMENT_ENVIRONMENT) == null) {
                systemProps.setProperty(ARCHAIUS_DEPLOYMENT_ENVIRONMENT, appEnvironment);
            }
            ConfigurationManager.install(finalConfiguration);
        }
    }

    private void addServerInstanceProperties(Configuration configuration) {
        String instanceId = LOCAL_INSTANCE_ID;
        String region = UNKNOWN;
        String availabilityZone = UNKNOWN;
        String privateIp = "127.0.0.1";
        String publicIp = null;
        String instanceName = Joiner.on("-").join(appEnvironment, appName, appVersion);

        if (serverInstanceContext != null) {
            instanceId = serverInstanceContext.getInstanceId();
            region = serverInstanceContext.getRegion();
            availabilityZone = serverInstanceContext.getAvailabilityZone();
            privateIp = serverInstanceContext.getPrivateIp();
            publicIp = serverInstanceContext.getPublicIp();
        }

        configuration.addProperty(BootstrapConfigKeys.AWS_INSTANCE_ID.getPropertyName(), instanceId);
        configuration.addProperty(BootstrapConfigKeys.AWS_INSTANCE_REGION.getPropertyName(), region);
        configuration.addProperty(BootstrapConfigKeys.AWS_INSTANCE_AVAILABILITY_ZONE.getPropertyName(), availabilityZone);
        configuration.addProperty(BootstrapConfigKeys.AWS_INSTANCE_PRIVATE_IP.getPropertyName(), privateIp);
        if (publicIp != null) {
            configuration.addProperty(BootstrapConfigKeys.AWS_INSTANCE_PUBLIC_IP.getPropertyName(), publicIp);
        }
        configuration.addProperty(BootstrapConfigKeys.AWS_INSTANCE_NAME.getPropertyName(), instanceName);
    }

    private void logConfiguration(ConcurrentCompositeConfiguration configuration) {
        new LoggerConfigurationWriter(LOGGER).write(configuration, null);
    }

    private void checkAppVersion() {
        if (StringUtils.isBlank(appVersion)) {
            BootstrapException.missingApplicationVersion();
        }
    }

    private void initModuleConfiguration() {
        if (!scanModuleConfigurations) {
            this.moduleDefaultConfiguration = new ConcurrentMapConfiguration();
            return;
        }
        HashMap<String, Object> base = new HashMap<>();
        Set<Class<?>> types = reflections
                .getTypesAnnotatedWith(PropertySource.class);
        for (Class<?> type : types) {
            PropertySource propertySource = type
                    .getAnnotation(PropertySource.class);
            String[] propertiesFiles = propertySource.value();
            for (String propertyFile : propertiesFiles) {
                Properties properties = new Properties();
                try (InputStream is = resourceLoader.getResource(SystemPropertyUtils.resolvePlaceholders(propertyFile))
                        .getInputStream()) {
                    properties.load(is);
                    LOGGER.debug("Initializing module properties from path " + propertyFile);
                } catch (Exception e) {
                    BootstrapException.resourceLoadingFailed(propertyFile, e);
                }
                join(base, properties, propertyFile, propertiesFiles);
            }
        }
        this.moduleDefaultConfiguration = new ConcurrentMapConfiguration(base);
    }

    //get the application configuration from the provider, possibly publishing module defaults
    private void initApplicationConfiguration() {
        if (configurationProvider == null) {
            return;
        }
        try {
            this.applicationConfiguration = configurationProvider.getApplicationConfiguration(appEnvironment, appName, appVersion, serverInstanceContext);
        } catch (ApplicationConfigurationNotFoundException e) {
            if (this.publishDefaults) {
                publishDefaults();
                initApplicationConfiguration();
            } else{
                throw e;
            }
        }
    }

    private void publishDefaults() {
        ConfigurationBuilder defaultsBuilder = new ConfigurationBuilder(appName, appEnvironment, addSystemConfigs, reflections);
        defaultsBuilder.withAppVersion(appVersion);
        //a bit of a hack to get around Archaius's singleton requirement
        defaultsBuilder.configureArchaius = false;
        if (applicationPropertiesPath != null) {
            defaultsBuilder.withApplicationProperties(applicationPropertiesPath);
        }
        if (serverInstanceContext != null) {
            defaultsBuilder.withServerInstanceContext(serverInstanceContext);
        }
        defaultsBuilder.withScanModuleConfigurations(scanModuleConfigurations);
        configurationProvider.writeApplicationConfiguration(appEnvironment, appName, appVersion, defaultsBuilder.build(), false);
    }

    private String buildZookeeperApplicationBasePath() {
        return "/" + appEnvironment + "/" + appName + "/" + appVersion;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initApplicationFileConfiguration() {
        if (applicationPropertiesPath == null) {
            LOGGER.debug("No client application properties to configure. Skipping...");
            applicationFileConfiguration = new ConcurrentMapConfiguration();
            return;
        }

        this.applicationFileConfiguration = new ConcurrentCompositeConfiguration();

        String path = SystemPropertyUtils.resolvePlaceholders(applicationPropertiesPath);
        LOGGER.debug("Configuring client application properties from path " + applicationPropertiesPath);

        Map applicationProperties = new Properties();

        if (SystemUtils.IS_OS_WINDOWS) {
            if (path.startsWith("file://")) {
                if (!path.startsWith("file:///")) {
                    path = path.replaceFirst(Pattern.quote("file://"), "file:///");
                }
            }
        }

        try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
            ((Properties) applicationProperties).load(is);
        } catch (Exception e) {
            BootstrapException.resourceLoadingFailed(path, applicationPropertiesPath, e);
        }

        Map environmentApplicationProperties = getEnvironmentSpecificProperties(path);
        if (environmentApplicationProperties != null) {
            ((ConcurrentCompositeConfiguration) this.applicationFileConfiguration).addConfiguration(new ConcurrentMapConfiguration(environmentApplicationProperties));
        }
        ((ConcurrentCompositeConfiguration) this.applicationFileConfiguration).addConfiguration(new ConcurrentMapConfiguration(applicationProperties));

        if (applicationFileConfiguration.containsKey(BootstrapConfigKeys.PUBLISH_DEFAULTS_KEY.getPropertyName())) {
            this.publishDefaults = applicationFileConfiguration.getBoolean(BootstrapConfigKeys.PUBLISH_DEFAULTS_KEY.getPropertyName());
        }
    }

    @SuppressWarnings("rawtypes")
    private Map getEnvironmentSpecificProperties(String path) {
        path = path.replace(".properties", "." + this.appEnvironment + ".properties");

        try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
            Properties properties = new Properties();
            properties.load(is);
            LOGGER.debug("Configuration client application properties from path " + path);
            return properties;
        } catch (FileNotFoundException e) {
            LOGGER.debug("Attempted to load environment specific client application configuration at path " + path + " but didn't find one. skipping...");
            return null;
        } catch (Exception e) {
            BootstrapException.resourceLoadingFailed(path, e);
            return null;
        }
    }

    private void initAppVersion() {
        if (applicationFileConfiguration == null) {
            throw new BootstrapException("client application configuration is null.");
        }
        if (appVersion == null) {
            if (System.getProperty(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName()) != null) {
                this.appVersion = System.getProperty(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName());
                return;
            }
            if (applicationFileConfiguration != null && applicationFileConfiguration.containsKey(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName())) {
                System.setProperty(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName(), applicationFileConfiguration.getString(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName()));
            }
        }
        if (applicationFileConfiguration.containsKey(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName())) {
            this.appVersion = applicationFileConfiguration.getString(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName());
        }
        checkAppVersion();
    }

    public ConfigurationBuilder withServerInstanceContext(ServerInstanceContext serverInstanceContext) {
        this.serverInstanceContext = serverInstanceContext;
        return this;
    }

    public ConfigurationBuilder withApplicationProperties(String path) {
        this.applicationPropertiesPath = path;
        return this;
    }

    public ConfigurationBuilder withAppVersion(String appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    public ConfigurationBuilder withScanModuleConfigurations(boolean scanModuleConfigurations) {
        this.scanModuleConfigurations = scanModuleConfigurations;
        return this;
    }

    public ConfigurationBuilder withConfigurationProvider(ConfigurationProvider configurationProvider) {
        this.configurationProvider = configurationProvider;
        return this;
    }

    @Override
    public void close() throws IOException {
        if (configurationProvider != null) {
            configurationProvider.close();
        }
    }
}
