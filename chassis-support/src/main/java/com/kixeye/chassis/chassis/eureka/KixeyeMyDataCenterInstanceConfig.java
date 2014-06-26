package com.kixeye.chassis.chassis.eureka;

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
