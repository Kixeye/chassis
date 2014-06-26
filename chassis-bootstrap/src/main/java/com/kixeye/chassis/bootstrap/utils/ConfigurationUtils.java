package com.kixeye.chassis.bootstrap.utils;

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
