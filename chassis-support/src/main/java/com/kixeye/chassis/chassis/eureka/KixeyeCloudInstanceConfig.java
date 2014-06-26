package com.kixeye.chassis.chassis.eureka;

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
