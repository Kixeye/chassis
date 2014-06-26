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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.kixeye.chassis.bootstrap.utils.ConfigurationUtils;

/**
 * Writes application configurations to Zookeeper
 *
 * @author dturner@kixeye.com
 */
public class ZookeeperConfigurationWriter implements ConfigurationWriter {
    private static Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfigurationWriter.class);

    private String configPath;
    private CuratorFramework curatorFramework;
    private boolean allowOverwrite;

    public ZookeeperConfigurationWriter(
            String applicationName,
            String environment,
            String version,
            CuratorFramework curatorFramework,
            boolean allowOverwrite) {
        Preconditions.checkArgument(StringUtils.isNotBlank(applicationName), "name is required");
        Preconditions.checkArgument(StringUtils.isNotBlank(environment), "environment is required");
        Preconditions.checkArgument(StringUtils.isNotBlank(version), "version is required");

        this.configPath = String.format("/%s/%s/%s/config", environment, applicationName, version);
        this.curatorFramework = curatorFramework;
        this.allowOverwrite = allowOverwrite;
    }

    @Override
    public void write(Configuration configuration, PropertyFilter propertyFilter) {
        Map<String, ?> config = ConfigurationUtils.copy(configuration);
        boolean exists = validateConfigPathAbsent();
        if (!exists) {
            writeConfigPath(configPath);
        }
        ArrayList<Set<String>> deltas = findDeltas(config);
        for (String key : deltas.get(0)) {
            String value = config.get(key) + "";
            String path = configPath + "/" + key;
            writeKey(path, value, propertyFilter);
        }
        for (String key : deltas.get(1)) {
            deleteKey(configPath + "/" + key);
        }
    }

    private void deleteKey(String key) {
        try {
            LOGGER.info("deleting key {}...", key);
            curatorFramework.delete().forPath(key);
        } catch (Exception e) {
            throw new BootstrapException("Failed to delete key " + key, e);
        }
    }

    private ArrayList<Set<String>> findDeltas(Map<String, ?> configuration) {
        Set<String> keysToWrite = new TreeSet<>();
        Set<String> keysToDelete = new HashSet<>();

        List<String> existingKeys;
        try {
            existingKeys = curatorFramework.getChildren().forPath(configPath);
        } catch (Exception e) {
            throw new BootstrapException("Unable to determine existing keys for path " + configPath, e);
        }

        //figures out which existing keys to keep and which to delete
        for (String existingKey : existingKeys) {
            if (configuration.containsKey(existingKey)) {
                keysToWrite.add(existingKey);
            } else {
                keysToDelete.add(existingKey);
            }
        }

        //add any new keys
        for (String configKey : configuration.keySet()) {
            keysToWrite.add(configKey);
        }

        ArrayList<Set<String>> deltas = new ArrayList<>(2);
        deltas.add(keysToWrite);
        deltas.add(keysToDelete);
        return deltas;
    }

    private void writeConfigPath(String path) {
        if (checkExists(path)) {
            return;
        }
        String next = path.substring(0, path.lastIndexOf("/"));
        if (StringUtils.isBlank(next)) {
            next = "/";
        }
        writeConfigPath(next);
        writeKey(path, null, null);
    }

    private void writeKey(String key, String value, PropertyFilter filter) {
        if(filter != null && filter.excludeProperty(key.substring(key.lastIndexOf("/")+1), value)){
            LOGGER.debug("Filtering out key {} with value {}", key, value);
            return;
        }
        try {
            if (!addKey(key, value)) {
                updateKey(key, value);
            }
        } catch (Exception e) {
            throw new BootstrapException("Failed to create or update key " + key + " with value " + value, e);
        }
    }

    private void updateKey(String key, String value) throws Exception {
        LOGGER.debug("updating key {} with value {}", key, value);
        if (value == null) {
            curatorFramework.setData().forPath(key);
            return;
        }
        curatorFramework.setData().forPath(key, value.getBytes());
    }

    private boolean addKey(String key, String value) throws Exception {
        try {
            LOGGER.debug("adding key {} with value {}", key, value);
            if (value == null) {
                curatorFramework.create().forPath(key);
                return true;
            }
            curatorFramework.create().forPath(key, value.getBytes());
            return true;
        } catch (NodeExistsException e) {
            LOGGER.debug("cannot add key {} because it already exists", key);
            return false;
        }
    }

    private boolean validateConfigPathAbsent() {
        LOGGER.debug("checking to see if {} exists...", configPath);
        boolean exists = checkExists(configPath);
        LOGGER.debug("{} exists? {}", configPath, exists);
        if (exists) {
            if (allowOverwrite) {
                LOGGER.warn("Zookeeper config path {} already exists, but will be overwritten by force.", configPath);
                return exists;
            }
            throw new BootstrapException("Configuration path " + configPath + " already exists. Refusing to write configuration to zookeeper.");
        }
        return exists;
    }

    private boolean checkExists(String path) {
        try {
            return curatorFramework.checkExists().forPath(path) != null;
        } catch (Exception e) {
            throw new BootstrapException("Failed to check existence for key " + configPath, e);
        }
    }
}
