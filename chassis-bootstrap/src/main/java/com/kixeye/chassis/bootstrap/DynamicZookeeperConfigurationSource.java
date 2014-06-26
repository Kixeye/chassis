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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.listen.ListenerContainer;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.netflix.config.WatchedConfigurationSource;
import com.netflix.config.WatchedUpdateListener;
import com.netflix.config.WatchedUpdateResult;
import com.netflix.config.source.ZooKeeperConfigurationSource;

/**
 * Watches Zookeeper for a specific path to be created or removed, and configures a
 * ZookeeperConfigurationSource when one of the above events occurs.
 *
 * @author dturner@kixeye.com
 */
public class DynamicZookeeperConfigurationSource implements WatchedConfigurationSource, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicZookeeperConfigurationSource.class);
    private CuratorFramework curatorFramework;
    private ZooKeeperConfigurationSource zooKeeperConfigurationSource;
    private ListenerContainer<WatchedUpdateListener> listeners = new ListenerContainer<>();
    private PathChildrenCache pathChildrenCache;
    private String instanceConfigPath;
    private String configPathRoot;
    private volatile boolean running = false;

    public DynamicZookeeperConfigurationSource(CuratorFramework curatorFramework, final String configPathRoot, final String configNode) {
        this.curatorFramework = curatorFramework;
        this.configPathRoot = configPathRoot;
        this.instanceConfigPath = this.configPathRoot + "/" + configNode;

        LOGGER.debug("Configuring dynamic zookeeper config source for path {}", instanceConfigPath);

        initializePathChildrenCache();
        initializeZookeeperConfigurationSourceIfNecessary();
    }

    private void initializeZookeeperConfigurationSourceIfNecessary() {
        boolean exists = false;
        try {
            exists = curatorFramework.checkExists().forPath(instanceConfigPath) != null;
        } catch (Exception e) {
            throw new BootstrapException("Failed to check existence of path " + instanceConfigPath, e);
        }
        if (exists) {
            initializeZookeeperConfigurationSource();
        }
    }

    private void initializePathChildrenCache() {
        pathChildrenCache = new PathChildrenCache(curatorFramework, configPathRoot, true);

        pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                LOGGER.debug("Got event {}", event);
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED && event.getData().getPath().equals(instanceConfigPath)) {
                    //do stuff
                    LOGGER.info("Detected creation of node {}. Initializing zookeeper configuration source...", instanceConfigPath);
                    try {
                        initializeZookeeperConfigurationSource();
                    } catch (BootstrapException e) {
                        LOGGER.error("Failed to initialized zookeeper configuration source for path " + instanceConfigPath, e);
                        throw e;
                    }
                    return;
                }
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED && event.getData().getPath().equals(instanceConfigPath)) {
                    if (running) {
                        LOGGER.info("Detected deletion of node {}, destroying zookeeper configuration source...", instanceConfigPath);
                        destroyZookeeperCofigurationSource();
                        return;
                    }
                    LOGGER.warn("Detected deletion of node {}, but zookeeper configuration source not currently running. This should not happen. Ignoring event...", instanceConfigPath);
                    return;
                }
                LOGGER.debug("Ignoring event {}", event);
            }
        });
        try {
            pathChildrenCache.start(StartMode.BUILD_INITIAL_CACHE);
        } catch (Exception e) {
            throw new BootstrapException("Failed to initialize zookeeper configuration path cache" + configPathRoot, e);
        }
    }
/*
    private boolean pathExists(String pathToWatchFor) {
        try {
            return curatorFramework.checkExists().forPath(pathToWatchFor) != null;
        } catch (Exception e) {
            throw new BootstrapException("Failed to check existence for path " + pathToWatchFor, e);
        }
    }
*/
    private void initializeZookeeperConfigurationSource() {
        if (running) {
            LOGGER.warn("Detected creation of node {}, but zookeeper configuration source already running. This should not happen. Ignoring event...", instanceConfigPath);
            return;
        }
        this.zooKeeperConfigurationSource = new ZooKeeperConfigurationSource(curatorFramework, instanceConfigPath);
        listeners.forEach(new Function<WatchedUpdateListener, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable WatchedUpdateListener watchedUpdateListener) {
                zooKeeperConfigurationSource.addUpdateListener(watchedUpdateListener);
                return null;
            }
        });
        try {
            zooKeeperConfigurationSource.start();
        } catch (Exception e) {
            LOGGER.error("errro starting zookeeper configuration source", e);
            throw new BootstrapException("Error initializing zookeeper configuration source", e);
        }
        running = true;
    }

    private void destroyZookeeperCofigurationSource() {
        running = false;
        zooKeeperConfigurationSource.close();
        zooKeeperConfigurationSource = null;
        //tell all the listeners that there is no more data from this source.
        listeners.forEach(new Function<WatchedUpdateListener, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable WatchedUpdateListener watchedUpdateListener) {
                watchedUpdateListener.updateConfiguration(WatchedUpdateResult.createFull(new HashMap<String, Object>()));
                return null;
            }
        });
    }

    @Override
    public void close() throws IOException {
        if (zooKeeperConfigurationSource != null) {
            zooKeeperConfigurationSource.close();
        }
    }

    @Override
    public void addUpdateListener(WatchedUpdateListener listener) {
        listeners.addListener(listener);
        if (running) {
            zooKeeperConfigurationSource.addUpdateListener(listener);
        }
    }

    @Override
    public void removeUpdateListener(WatchedUpdateListener listener) {
        listeners.removeListener(listener);
        if (running) {
            zooKeeperConfigurationSource.removeUpdateListener(listener);
        }
    }

    @Override
    public Map<String, Object> getCurrentData() throws Exception {
        if (!running) {
            return new HashMap<>();
        }
        return zooKeeperConfigurationSource.getCurrentData();
    }

}
