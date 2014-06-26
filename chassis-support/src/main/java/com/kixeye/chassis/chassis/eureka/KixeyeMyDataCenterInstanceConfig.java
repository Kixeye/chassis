package com.kixeye.chassis.chassis.eureka;

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

import java.util.Map;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;

public class KixeyeMyDataCenterInstanceConfig extends MyDataCenterInstanceConfig {

    private final MetadataCollector collector;

    public KixeyeMyDataCenterInstanceConfig(MetadataCollector collector) {
        this.collector = collector;
    }

    public KixeyeMyDataCenterInstanceConfig(MetadataCollector collector, String namespace) {
        super(namespace);
        this.collector = collector;
    }

    public KixeyeMyDataCenterInstanceConfig(MetadataCollector collector, String namespace,DataCenterInfo dataCenterInfo) {
        super(namespace, dataCenterInfo);
        this.collector = collector;
    }

    /***
     * There is a bug in PropertiesInstanceConfig.init() adds an extra '.' in propMetadataNamespace
     * which in turn breaks PropertiesInstanceConfig.getMetadataMap.  Annoyingly, PropertiesInstanceConfig
     * made propMetadataNamespace private so we have to override the whole function.
     *
     * @return Map of meta data key,value pairs
     */
    @Override
    public Map<String, String> getMetadataMap() {
        return collector.getMetadataMap();
    }
}
