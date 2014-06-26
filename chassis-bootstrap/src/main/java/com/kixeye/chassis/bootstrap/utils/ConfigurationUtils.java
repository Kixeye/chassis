package com.kixeye.chassis.bootstrap.utils;

import org.apache.commons.configuration.Configuration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utils for working with Configurations
 *
 * @author dturner@kixeye.com
 */
public class ConfigurationUtils {

    public static Map<String, ?> copy(Configuration configuration){
        Map<String, Object> config = new TreeMap<>();
        Iterator<String> keys = configuration.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            config.put(key, configuration.getProperty(key));
        }
        return config;
    }
}
