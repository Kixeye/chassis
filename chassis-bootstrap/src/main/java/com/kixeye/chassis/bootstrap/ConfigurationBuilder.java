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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.SystemPropertyUtils;

import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.BootstrapException.MissingApplicationConfigurationInZookeeperException;
import com.kixeye.chassis.bootstrap.aws.AwsInstanceContext;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicWatchedConfiguration;
import com.netflix.config.source.ZooKeeperConfigurationSource;

import static com.kixeye.chassis.bootstrap.BootstrapConfigKeys.*;

/**
 * Builder class for creating the client application's configuration.  Configurations are hierarchical, and property lookup
 * evaluate will take place in the following order:
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
public class ConfigurationBuilder {
    public static final Reflections REFLECTIONS = new Reflections("com.kixeye");
    public static final String LOCAL_INSTANCE_ID = "local";
    public static final String UNKNOWN = "unknown";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationBuilder.class);
    private static final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
    private final static AbstractConfiguration defaults = new ConcurrentMapConfiguration();
    private final ConcurrentCompositeConfiguration configurationServerConnectionConfiguration = new ConcurrentCompositeConfiguration();
    private boolean scanModuleConfigurations = true;
    private boolean addSystemConfigs = true;
    private boolean publishDefaultsToZookeeper = false;
    private String applicationPropertiesPath;
    private CuratorFrameworkBuilder curatorFrameworkBuilder;
    private String appName;
    private String appEnvironment;
    private String appVersion;
    private AwsInstanceContext awsInstanceContext;
    private String configurationBasePath;
    private CuratorFramework curatorClient;
    private boolean configureArchaius = true;
    //properties from local client application file
    private AbstractConfiguration applicationConfiguration;
    //base properties loaded from zookeeper
    private AbstractConfiguration zookeeperInstanceConfiguration;
    //instanceMetadata properties loaded from zookeeper
    private AbstractConfiguration zookeeperBaseConfiguration;
    //properties loaded from dependency modules
    private AbstractConfiguration moduleDefaultConfiguration;

    static {
        defaults.addProperty(AWS_METADATA_TIMEOUTSECONDS.getPropertyName(), 2);
    }
    
    public ConfigurationBuilder(String appName, String appEnvironment, boolean addSystemConfigs) {
        Preconditions.checkArgument(StringUtils.isNotBlank(appName));
        Preconditions.checkArgument(StringUtils.isNotBlank(appEnvironment));
        this.appName = appName;
        this.appEnvironment = appEnvironment;
        this.addSystemConfigs = addSystemConfigs;

        System.setProperty(APP_NAME_KEY.getPropertyName(), appName);
        System.setProperty(APP_ENVIRONMENT_KEY.getPropertyName(), appEnvironment);
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
    
    public void close() {
    	if (curatorClient != null) {
    		curatorClient.close();
    	}
    }

    /**
     * Build the Configuration
     *
     * @return the configuration
     */
    public AbstractConfiguration build() {
        initApplicationConfiguration();
        initAppVersion();
        initConfigurationServerConnectionConfiguration();
        initZookeeperConfiguration();
        initModuleConfiguration();

        ConcurrentCompositeConfiguration configuration = new ConcurrentCompositeConfiguration();
        if (addSystemConfigs) {
            configuration.addConfiguration(new ConcurrentMapConfiguration(new SystemConfiguration()));
        }

        configuration.addProperty(APP_VERSION_KEY.getPropertyName(), appVersion);

        addInstanceProperties(configuration);

        if (zookeeperBaseConfiguration == null) {
            LOGGER.warn("\n\n    ****** Default configuration being used ******\n    client application \"" + appName + "\" is being configured with modules defaults. Defaults should only be used in development environments.\n    In non-developement environments, Zookeeper should be used to configure the client application and it should define ALL configurations.\n");
            //not using zookeeper, so load client application config, then module defaults.
            configuration.addConfiguration(applicationConfiguration);
            configuration.addConfiguration(moduleDefaultConfiguration);
        } else {
            //can't initialize zookeeper instanceMetadata config until after we've fetched AwsUtils instanceMetadata meta-data.
            initZookeeperInstanceConfiguration();
            //using zookeeper, and we expect all module requirements be configured there. (no module defaults are used)
            configuration.addConfiguration(zookeeperInstanceConfiguration);
            configuration.addConfiguration(zookeeperBaseConfiguration);
            configuration.addConfiguration(applicationConfiguration);
        }

        configuration.addConfiguration(configurationServerConnectionConfiguration);
        configuration.addConfiguration(CuratorFrameworkBuilder.defaults);

        if (configurationBasePath != null) {
            configuration.addProperty(ZOOKEEPER_CONFIG_BASE_PATH.getPropertyName(), configurationBasePath);
        }

        if(configureArchaius){
            ConfigurationManager.install(configuration);
        }

        logConfiguration(configuration);

        return configuration;
    }

    private void addInstanceProperties(Configuration configuration) {
        String instanceId = LOCAL_INSTANCE_ID;
        String region = UNKNOWN;
        String availabilityZone = UNKNOWN;
        String privateIp = "127.0.0.1";
        String publicIp = null;
        String instanceName = Joiner.on("-").join(appEnvironment, appName, appVersion);

        if (awsInstanceContext != null) {
            instanceId = awsInstanceContext.getInstanceId();
            region = awsInstanceContext.getRegion();
            availabilityZone = awsInstanceContext.getAvailabilityZone();
            privateIp = awsInstanceContext.getPrivateIp();
            publicIp = awsInstanceContext.getPublicIp();
        }

        configuration.addProperty(AWS_INSTANCE_ID.getPropertyName(), instanceId);
        configuration.addProperty(AWS_INSTANCE_REGION.getPropertyName(), region);
        configuration.addProperty(AWS_INSTANCE_AVAILABILITY_ZONE.getPropertyName(), availabilityZone);
        configuration.addProperty(AWS_INSTANCE_PRIVATE_IP.getPropertyName(), privateIp);
        if (publicIp != null) {
            configuration.addProperty(AWS_INSTANCE_PUBLIC_IP.getPropertyName(), publicIp);
        }
        configuration.addProperty(AWS_INSTANCE_NAME.getPropertyName(), instanceName);

    }

    private void logConfiguration(ConcurrentCompositeConfiguration configuration) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Configuring service with configuration properties:");
        Iterator<String> keys = configuration.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            sb.append("\n    ").append(key).append("=").append(configuration.getProperty(key));
        }
        LOGGER.debug(sb.toString());
    }

    //initializes a separate hierarchy for zookeeper connection properties.
    private void initConfigurationServerConnectionConfiguration() {
        configurationServerConnectionConfiguration.addConfiguration(new ConcurrentMapConfiguration(new SystemConfiguration()));
        configurationServerConnectionConfiguration.addConfiguration(applicationConfiguration);
        configurationServerConnectionConfiguration.addConfiguration(defaults);
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
        Set<Class<?>> types = REFLECTIONS
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

    private void initZookeeperConfiguration() {
        if (curatorFrameworkBuilder == null) {
            return;
        }
        initializeCuratorClient();
        buildZookeeperConfigBasePath();

        ZooKeeperConfigurationSource source = new ZooKeeperConfigurationSource(curatorClient, configurationBasePath);

        try {
            source.start();
        } catch (Exception e) {
            source.close();
            BootstrapException.zookeeperInitializationFailed(curatorFrameworkBuilder.getZookeeperConnectionString(), configurationBasePath, e);
        }

        LOGGER.debug("Initializing zookeeper configuration from host " + curatorFrameworkBuilder.getZookeeperConnectionString() + " at path " + configurationBasePath);

        this.zookeeperBaseConfiguration = new DynamicWatchedConfiguration(source);
    }

    private void initZookeeperInstanceConfiguration() {
        String instanceId = awsInstanceContext == null ? LOCAL_INSTANCE_ID : awsInstanceContext.getInstanceId();
        this.zookeeperInstanceConfiguration =
                new DynamicWatchedConfiguration(
                        new DynamicZookeeperConfigurationSource(curatorClient, buildZookeeperApplicationBasePath(), instanceId + "-config"));
    }

    private void buildZookeeperConfigBasePath() {
        configurationBasePath = buildZookeeperApplicationBasePath() + "/config";

        List<String> children;
        try {
            children = getChildren(configurationBasePath);
        } catch (MissingApplicationConfigurationInZookeeperException e) {
            if(publishDefaultsToZookeeper){
                LOGGER.debug("Found no configurations in Zookeeper. Publishing the application's defaults to Zookeeper");
                publishDefaultsToZookeeper();
                children = getChildren(configurationBasePath);
            } else {
                throw e;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Found the following configuration keys in zookeeper:\n").append(configurationBasePath);
        for (String child : children) {
            sb.append("\n    /").append(child);
        }
        LOGGER.debug(sb.toString());
    }

    private List<String> getChildren(String path){
        try {
            return curatorClient.getChildren().forPath(path);
        } catch(NoNodeException e){
            BootstrapException.configurationMissingFromZookeeper(curatorFrameworkBuilder.getZookeeperConnectionString(), configurationBasePath, e);
            return null;
        } catch (Exception e) {
            throw new BootstrapException("Failed to fetch children for path " + path, e);
        }
    }

	private void publishDefaultsToZookeeper() {
        ConfigurationBuilder defaultsBuilder = new ConfigurationBuilder(appName, appEnvironment, addSystemConfigs);
        defaultsBuilder.withAppVersion(appVersion);
        //a bit of a hack to get around Archaius's singleton requirement
        defaultsBuilder.configureArchaius = false;
        if(applicationPropertiesPath != null){
            defaultsBuilder.withApplicationProperties(applicationPropertiesPath);
        }
        if(awsInstanceContext != null){
            defaultsBuilder.withAwsInstanceContext(awsInstanceContext);
        }
        if(!scanModuleConfigurations){
            defaultsBuilder.withoutModuleScanning();
        }
        AbstractConfiguration defaults = defaultsBuilder.build();
        ZookeeperConfigurationWriter configurationWriter =
                new ZookeeperConfigurationWriter(appName, appEnvironment, appVersion, curatorClient, false);
        configurationWriter.write(defaults, new DefaultPropertyFilter());
    }

    private String buildZookeeperApplicationBasePath() {
        return "/" + appEnvironment + "/" + appName + "/" + appVersion;
    }

    public void initializeCuratorClient() {
        curatorFrameworkBuilder.withConfiguration(applicationConfiguration);
        curatorClient = curatorFrameworkBuilder.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initApplicationConfiguration() {
        if (applicationPropertiesPath == null) {
            LOGGER.debug("No client application properties to configure. Skipping...");
            applicationConfiguration = new ConcurrentMapConfiguration();
            return;
        }

        this.applicationConfiguration = new ConcurrentCompositeConfiguration();

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
            ((ConcurrentCompositeConfiguration) this.applicationConfiguration).addConfiguration(new ConcurrentMapConfiguration(environmentApplicationProperties));
        }
        ((ConcurrentCompositeConfiguration) this.applicationConfiguration).addConfiguration(new ConcurrentMapConfiguration(applicationProperties));

        if(this.applicationConfiguration.containsKey(BootstrapConfigKeys.PUBLISH_DEFAULTS_TO_ZOOKEEPER_KEY.getPropertyName())){
            this.publishDefaultsToZookeeper = this.applicationConfiguration.getBoolean(BootstrapConfigKeys.PUBLISH_DEFAULTS_TO_ZOOKEEPER_KEY.getPropertyName());
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
        if (applicationConfiguration == null) {
            throw new BootstrapException("client application configuration is null.");
        }
        if (appVersion == null) {
            if (System.getProperty(APP_VERSION_KEY.getPropertyName()) != null) {
                this.appVersion = System.getProperty(APP_VERSION_KEY.getPropertyName());
                return;
            }
            if (applicationConfiguration != null && applicationConfiguration.containsKey(APP_VERSION_KEY.getPropertyName())) {
                System.setProperty(APP_VERSION_KEY.getPropertyName(), applicationConfiguration.getString(APP_VERSION_KEY.getPropertyName()));
            }
        }
        if (applicationConfiguration.containsKey(APP_VERSION_KEY.getPropertyName())) {
            this.appVersion = applicationConfiguration.getString(APP_VERSION_KEY.getPropertyName());
        }
        checkAppVersion();
    }

    public ConfigurationBuilder withAwsInstanceContext(AwsInstanceContext awsInstanceContext){
        Preconditions.checkNotNull(awsInstanceContext);
        this.awsInstanceContext = awsInstanceContext;
        return this;
    }

    public ConfigurationBuilder withApplicationProperties(String path) {
        Preconditions.checkArgument(StringUtils.isNotBlank(path));
        this.applicationPropertiesPath = path;
        return this;
    }

    public ConfigurationBuilder withAppVersion(String appVersion) {
        Preconditions.checkArgument(StringUtils.isNotBlank(appVersion));
        this.appVersion = appVersion;
        return this;
    }

    public ConfigurationBuilder withZookeeper(String host) {
        initCuratorFrameworkBuilderIfNecessary();
        curatorFrameworkBuilder.withZookeeper(host);
        return this;
    }

    public ConfigurationBuilder withExhibitors(int port, final String... exhibitors) {
        initCuratorFrameworkBuilderIfNecessary();
        curatorFrameworkBuilder.withExhibitors(port, exhibitors);
        return this;
    }

    private void initCuratorFrameworkBuilderIfNecessary() {
        if (curatorFrameworkBuilder == null) {
            curatorFrameworkBuilder = new CuratorFrameworkBuilder(true);
        }
    }

    public ConfigurationBuilder withoutModuleScanning() {
        this.scanModuleConfigurations = false;
        return this;
    }

}
