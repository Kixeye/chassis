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

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.kixeye.chassis.bootstrap.BootstrapException;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.exhibitor.ExhibitorEnsembleProvider;
import org.apache.curator.ensemble.exhibitor.Exhibitors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Arrays;

import static com.kixeye.chassis.bootstrap.configuration.BootstrapConfigKeys.*;

/**
 * Builds a CuratorFramework instance from Zookeeper OR Exhibitor.
 *
 * @author dturner@kixeye.com
 */
public class CuratorFrameworkBuilder {
    public final static AbstractConfiguration defaults = new ConcurrentMapConfiguration();
    private static final Logger LOGGER = LoggerFactory.getLogger(CuratorFrameworkBuilder.class);
    private String zookeeperConnectionString;
    private Exhibitors exhibitors;
    private AbstractConfiguration configuration = new ConcurrentMapConfiguration();
    private boolean startOnBuild;

    static {
        defaults.addProperty(ZOOKEEPER_MAX_RETRIES.getPropertyName(), 29);
        defaults.addProperty(ZOOKEEPER_INITIAL_SLEEP_MILLIS.getPropertyName(), 1000);
        defaults.addProperty(ZOOKEEPER_RETRIES_MAX_MILLIS.getPropertyName(), 60000);
        defaults.addProperty(ZOOKEEPER_SESSION_TIMEOUT_MILLIS.getPropertyName(), 60000);
        defaults.addProperty(ZOOKEEPER_CONNECTION_TIMEOUT_MILLIS.getPropertyName(), 60000);

        defaults.addProperty(EXHIBITOR_POLL_INTERVAL.getPropertyName(), 30000);
        defaults.addProperty(EXHIBITOR_MAX_RETRIES.getPropertyName(), 29);
        defaults.addProperty(EXHIBITOR_INITIAL_SLEEP_MILLIS.getPropertyName(), 60000);
        defaults.addProperty(EXHIBITOR_RETRIES_MAX_MILLIS.getPropertyName(), 60000);
        defaults.addProperty(EXHIBITOR_URI_PATH.getPropertyName(), "/exhibitor/v1/cluster/list");
        defaults.addProperty(EXHIBITOR_USE_HTTPS.getPropertyName(), false);
    }

    public CuratorFrameworkBuilder(boolean startOnBuild) {
        this.startOnBuild = startOnBuild;
    }

    public CuratorFrameworkBuilder withZookeeper(String zookeeperConnectionString) {
        Preconditions.checkArgument(StringUtils.isNotBlank(zookeeperConnectionString));
        if (this.exhibitors != null) {
            BootstrapException.zookeeperExhibitorConflict();
        }
        this.zookeeperConnectionString = zookeeperConnectionString;
        return this;
    }

    public CuratorFrameworkBuilder withExhibitors(int port, final String... exhibitors) {
        Preconditions.checkNotNull(exhibitors);
        Preconditions.checkArgument(exhibitors.length > 0);
        Preconditions.checkArgument(port > 0);
        if (this.zookeeperConnectionString != null) {
            BootstrapException.zookeeperExhibitorConflict();
        }
        this.exhibitors = new Exhibitors(Arrays.asList(exhibitors), port, new Exhibitors.BackupConnectionStringProvider() {
            @Override
            public String getBackupConnectionString() throws Exception {
                //no backup zookeeper connection string
                return "";
            }
        });
        return this;
    }

    public CuratorFrameworkBuilder withConfiguration(AbstractConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    private CuratorFramework buildCuratorWithZookeeperDirectly(Configuration configuration) {
        LOGGER.debug("configuring direct zookeeper connection.");
        
        CuratorFramework curator = CuratorFrameworkFactory.newClient(
                this.zookeeperConnectionString,
                configuration.getInt(ZOOKEEPER_SESSION_TIMEOUT_MILLIS.getPropertyName()),
                configuration.getInt(ZOOKEEPER_CONNECTION_TIMEOUT_MILLIS.getPropertyName()),
                buildZookeeperRetryPolicy(configuration));
        curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
			public void stateChanged(CuratorFramework client, ConnectionState newState) {
				LOGGER.debug("Connection state to ZooKeeper changed: " + newState);
			}
		});
        
        return curator;
    }

    private CuratorFramework buildCuratorWithExhibitor(Configuration configuration) {
        LOGGER.debug("configuring zookeeper connection through Exhibitor...");
        ExhibitorEnsembleProvider ensembleProvider =
                new KixeyeExhibitorEnsembleProvider(
                        exhibitors,
                        new KixeyeExhibitorRestClient(configuration.getBoolean(EXHIBITOR_USE_HTTPS.getPropertyName())),
                        configuration.getString(EXHIBITOR_URI_PATH.getPropertyName()),
                        configuration.getInt(EXHIBITOR_POLL_INTERVAL.getPropertyName()),
                        new ExponentialBackoffRetry(
                                configuration.getInt(EXHIBITOR_INITIAL_SLEEP_MILLIS.getPropertyName()),
                                configuration.getInt(EXHIBITOR_MAX_RETRIES.getPropertyName()),
                                configuration.getInt(EXHIBITOR_RETRIES_MAX_MILLIS.getPropertyName())));

        //without this (undocumented) step, curator will attempt (and fail) to connect to a local instance of zookeeper (default behavior if no zookeeper connection string is provided) for
        //several seconds until the EnsembleProvider polls to get the SERVER list from Exhibitor. Polling before staring curator
        //ensures that the SERVER list from Exhibitor is already downloaded before curator attempts to connect to zookeeper.
        try {
            ensembleProvider.pollForInitialEnsemble();
        } catch (Exception e) {
            try {
                Closeables.close(ensembleProvider, true);
            } catch (IOException e1) {
            }
            throw new BootstrapException("Failed to initialize Exhibitor with host(s) " + exhibitors.getHostnames(), e);
        }
        
        CuratorFramework curator = CuratorFrameworkFactory.builder().ensembleProvider(ensembleProvider).retryPolicy(buildZookeeperRetryPolicy(configuration)).build();
        curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
			public void stateChanged(CuratorFramework client, ConnectionState newState) {
				LOGGER.debug("Connection state to ZooKeeper changed: " + newState);
			}
		});
        
        return curator;
    }

    private RetryPolicy buildZookeeperRetryPolicy(Configuration configuration) {
        return new ExponentialBackoffRetry(
                configuration.getInt(ZOOKEEPER_INITIAL_SLEEP_MILLIS.getPropertyName()),
                configuration.getInt(ZOOKEEPER_MAX_RETRIES.getPropertyName()),
                configuration.getInt(ZOOKEEPER_RETRIES_MAX_MILLIS.getPropertyName()));
    }

    public CuratorFramework build() {
        if (this.exhibitors == null && this.zookeeperConnectionString == null) {
            throw new BootstrapException("Cannot build a CuratorFramework instance because no Zookeeper or Exhibitor connection information was provided.");
        }
        ConcurrentCompositeConfiguration configuration = buildConfiguration();
        CuratorFramework curatorFramework = null;
        if (zookeeperConnectionString != null) {
            curatorFramework = buildCuratorWithZookeeperDirectly(configuration);
        } else {
            curatorFramework = buildCuratorWithExhibitor(configuration);
        }
        if (startOnBuild) {
            try {
                curatorFramework.start();
            } catch (Exception e) {
                BootstrapException.zookeeperInitializationFailed(this.zookeeperConnectionString, this.exhibitors, e);
            }
        }
        return curatorFramework;
    }

    private ConcurrentCompositeConfiguration buildConfiguration() {
        ConcurrentCompositeConfiguration configuration = new ConcurrentCompositeConfiguration();
        configuration.addConfiguration(new ConcurrentMapConfiguration(new SystemConfiguration()));
        configuration.addConfiguration(this.configuration);
        configuration.addConfiguration(defaults);
        return configuration;
    }

    public String getZookeeperConnectionString() {
        return zookeeperConnectionString;
    }
}
