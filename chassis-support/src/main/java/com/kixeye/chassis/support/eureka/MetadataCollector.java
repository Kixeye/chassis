package com.kixeye.chassis.support.eureka;

/*
 * #%L
 * Chassis Support
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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.DynamicPropertyFactory;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects metadata to include in service discovery.  It pulls
 * metadata from several configuration values and dynamically from
 * MetadataPublishers in the application context.
 *
 * TODO: Switch to a event based model instead of polling.
 */
@Component
public class MetadataCollector {
    private Map<String,String> staticMetaDataCache;
    private Map<String,String> oldMetaData;
    private List<MetadataPublisher> publishers;

    /**
     * Fallback default constructor if there are no MetadataPublisher beans
     */
    public MetadataCollector() {
        this.publishers = null;
    }

    @Autowired(required = false)
    public MetadataCollector(List<MetadataPublisher> publishers)
    {
        this.publishers = publishers;
    }


    /**
     * Get metadata from configuration data that does not change.
     *
     * @return Map of static meta data
     */
    public Map<String,String> getStaticMetadataMap() {
        if (staticMetaDataCache == null) {
            // add eureka.metadata.* properties
            staticMetaDataCache = new LinkedHashMap<>();
            Configuration config = (Configuration) DynamicPropertyFactory.getBackingConfigurationSource();
            for (Iterator<String> iter = config.subset("eureka.metadata").getKeys(); iter.hasNext();) {
                String key = iter.next();
                String value = config.getString("eureka.metadata." + key);
                staticMetaDataCache.put(key, value);
            }

            // add WebSocket port to meta data if available
            if (DynamicPropertyFactory.getInstance().getBooleanProperty("websocket.enabled",false).get()) {
                int port = DynamicPropertyFactory.getInstance().getIntProperty("websocket.port", -1).get();
                if (port > 0) {
                    staticMetaDataCache.put("websocketPort", "" + port);
                }
            }

            // add secure WebSocket port to meta data if available
            if (DynamicPropertyFactory.getInstance().getBooleanProperty("secureWebsocket.enabled",false).get()) {
                int port = DynamicPropertyFactory.getInstance().getIntProperty("secureWebsocket.port", -1).get();
                if (port > 0) {
                    staticMetaDataCache.put("secureWebsocketPort", "" + port);
                }
            }
        }
        return staticMetaDataCache;
    }


    /**
     * Get metadata published but the application that can dynamically change.
     *
     * @return Map of dynamic metadata
     */
    public Map<String,String> getDynamicMetadataMap() {
        if (publishers == null || publishers.size() <= 0) {
            return null;
        }
        LinkedHashMap<String,String> metadata = new LinkedHashMap<>();
        for (MetadataPublisher publisher : publishers) {
            metadata.putAll( publisher.getMetadataMap()  );
        }
        return metadata;
    }


    /***
     * Get combined static and dynamic metadata
     *
     * @return Map of all metadata
     */
    public Map<String, String> getMetadataMap() {
        Map<String, String> dynamic = getDynamicMetadataMap();
        if (dynamic == null) {
            return getStaticMetadataMap();
        }
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>(getStaticMetadataMap());
        metadata.putAll(dynamic);
        return metadata;
    }


    /**
     * Update Eurkea if the metadata has changed since the last poll.
     */
    @Scheduled(fixedDelay = 30000)
    private void updateMetaData() {
        // Early out if Eureka has NOT been initialized.
        InstanceInfo instanceInfo = ApplicationInfoManager.getInstance().getInfo();
        if (instanceInfo == null) {
            return;
        }

        // Early out if we only have static metadata since that has already
        // been reported to Eureka.
        Map<String, String> dynamic = getDynamicMetadataMap();
        if (dynamic == null) {
            return;
        }

        // Early out if dynamic metadata has not changed since the last poll.
        if (oldMetaData != null && dynamic.size() == oldMetaData.size()) {
            boolean different = false;
            for (Map.Entry<String,String> kvp : dynamic.entrySet()) {
                if (!kvp.getValue().equals(oldMetaData.get(kvp.getKey()))) {
                    different = true;
                    break;
                }
            }
            if (!different) {
                return;
            }
        }

        // Update the instance info which will eventually get replicated to the eureka servers.
        // Note that registerAppMetadata is additive so we just need to include the dynamic values
        // since it already has the static ones.
        oldMetaData = dynamic;
        ApplicationInfoManager.getInstance().registerAppMetadata( dynamic );
    }
}
