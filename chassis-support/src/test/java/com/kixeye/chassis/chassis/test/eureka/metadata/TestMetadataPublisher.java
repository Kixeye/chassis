package com.kixeye.chassis.chassis.test.eureka.metadata;

import com.kixeye.chassis.chassis.eureka.MetadataPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TestMetadataPublisher implements MetadataPublisher {

    @Override
    public Map<String, String> getMetadataMap() {
        Map<String,String> map = new HashMap<String,String>();
        map.put("test1","value1");
        map.put("test2","value2");
        return map;
    }
}