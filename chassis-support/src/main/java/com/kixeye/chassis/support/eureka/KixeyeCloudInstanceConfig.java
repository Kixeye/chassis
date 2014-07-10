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

import java.util.Map;

import com.netflix.appinfo.CloudInstanceConfig;

public class KixeyeCloudInstanceConfig extends CloudInstanceConfig {

    private final MetadataCollector collector;

    public KixeyeCloudInstanceConfig(MetadataCollector collector) {
        this.collector = collector;
    }

    public KixeyeCloudInstanceConfig(MetadataCollector collector, String namespace) {
        super(namespace);
        this.collector = collector;
    }

    @Override
    public Map<String, String> getMetadataMap() {
        return collector.getMetadataMap();
    }
}
