package com.kixeye.chassis.bootstrap.configuration.zookeeper;

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

import com.google.common.io.Closeables;
import com.kixeye.chassis.bootstrap.BootstrapException;
import com.kixeye.chassis.bootstrap.aws.ServerInstanceContext;
import com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider;
import com.kixeye.chassis.bootstrap.configuration.DefaultPropertyFilter;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.DynamicWatchedConfiguration;
import com.netflix.config.source.ZooKeeperConfigurationSource;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Provides configuration stored in Zookeeper at base path /{environment}/{application}/{application version}/config
 * and instance path /{environment}/{application}/{application version}/{instance-id}-config with instance specific config
 * taking precedence over the base config.
 *
 * @author dturner@kixeye.com
 */
public class ZookeeperConfigurationProvider implements ConfigurationProvider {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperConfigurationProvider.class);

    private String zookeeperConnectionString;
    private CuratorFramework curatorFramework;

    /**
     * Using this constructor, the provider will connect directly to Zookeeper using the given connection string
     *
     * @param zookeeperConnectionString the zookeeper connection string in the format {host}:{port}
     */
    public ZookeeperConfigurationProvider(String zookeeperConnectionString) {
        this(new CuratorFrameworkBuilder(true).withZookeeper(zookeeperConnectionString));
    }

    /**
     * Using this constructor, the provider will resolve the appropriate zookeeper connection information
     * by querying an exhibitor REST api.
     *
     * @param exhibitorPort  the tcp port that the Exhibitor web service is listening on
     * @param exhibitorHosts the server host that hte Exhibitor web service is running on
     * @see <a href="https://github.com/Netflix/exhibitor">https://github.com/Netflix/exhibitor</a>
     */
    public ZookeeperConfigurationProvider(int exhibitorPort, String... exhibitorHosts) {
        this(new CuratorFrameworkBuilder(true).withExhibitors(exhibitorPort, exhibitorHosts));
    }

    private ZookeeperConfigurationProvider(CuratorFrameworkBuilder curatorFrameworkBuilder) {
        this.curatorFramework = curatorFrameworkBuilder.build();
        this.zookeeperConnectionString = curatorFrameworkBuilder.getZookeeperConnectionString();
    }

    /**
     * @see ConfigurationProvider#getApplicationConfiguration(String, String, String, com.kixeye.chassis.bootstrap.aws.ServerInstanceContext)
     */
    @Override
    public AbstractConfiguration getApplicationConfiguration(String environment, String applicationName, String applicationVersion, ServerInstanceContext serverInstanceContext) {
        String instanceId = serverInstanceContext == null ? "local" : serverInstanceContext.getInstanceId();

        String configRoot = getPath(environment, applicationName, applicationVersion);
        String primaryConfigPath = configRoot += "/config";
        String instanceConfigNode = instanceId + "-config";

        checkPath(primaryConfigPath);

        ZooKeeperConfigurationSource source = new ZooKeeperConfigurationSource(curatorFramework, primaryConfigPath);

        try {
            source.start();
        } catch (Exception e) {
            source.close();
            BootstrapException.zookeeperInitializationFailed(zookeeperConnectionString, primaryConfigPath, e);
        }

        logger.debug("Initializing zookeeper configuration from host " + zookeeperConnectionString + " at path " + primaryConfigPath);

        ConcurrentCompositeConfiguration configuration = new ConcurrentCompositeConfiguration();
        configuration.addConfiguration(getServerInstanceSpecificApplicationConfiguration(configRoot, instanceConfigNode));
        configuration.addConfiguration(new DynamicWatchedConfiguration(source));

        return configuration;
    }

    private AbstractConfiguration getServerInstanceSpecificApplicationConfiguration(String configRoot, String instanceConfigNode) {
        return
                new DynamicWatchedConfiguration(
                        new DynamicZookeeperConfigurationSource(curatorFramework, configRoot, instanceConfigNode));
    }

    private String getPath(String environment, String applicationName, String applicationVersion) {
            return String.format("/%s/%s/%s", environment, applicationName, applicationVersion);
    }

    @Override
    public void writeApplicationConfiguration(String environment, String applicationName, String applicationVersion, AbstractConfiguration configuration, boolean allowOverwrite) {
        ZookeeperConfigurationWriter writer = new ZookeeperConfigurationWriter(applicationName, environment, applicationVersion, curatorFramework, allowOverwrite);
        writer.write(configuration, new DefaultPropertyFilter());
    }

    private void checkPath(String path) {
        try {
            curatorFramework.getChildren().forPath(path);
        } catch (NoNodeException e) {
            BootstrapException.configurationNotFound(String.format("Unable to verify Zookeeper configuration path %s. Zookeeper configuration path format is /{environment}/{app name}/{app version}. Please verify that you zookeeper (%s) has that path.", path, zookeeperConnectionString), e);
        } catch (Exception e) {
            throw new BootstrapException(String.format("Failed to fetch children for path %s on server %s", path, zookeeperConnectionString), e);
        }
    }

    public CuratorFramework getCuratorFramework() {
        return curatorFramework;
    }

    @Override
    public void close() throws IOException {
        Closeables.close(curatorFramework, false);
    }
}
