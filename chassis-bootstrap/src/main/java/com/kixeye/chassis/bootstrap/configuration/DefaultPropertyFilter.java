package com.kixeye.chassis.bootstrap.configuration;

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

import com.kixeye.chassis.bootstrap.configuration.ConfigurationWriter.Filter;

/**
 * Filters out system properties and internal bootstrap defined properties.
 *
 * @author dturner@kixeye.com
 */
public class DefaultPropertyFilter implements Filter {

    @Override
    public boolean excludeProperty(String key, Object value) {
        //exclude system props
        if (System.getProperty(key) != null) {
            return true;
        }
        //exclude internal bootstrap keys
        if (BootstrapConfigKeys.fromPropertyName(key) != null) {
            return true;
        }
        return false;
    }
}
