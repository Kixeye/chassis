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

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import java.util.Iterator;

/**
 * Writes a configuration using a Logger
 *
 * @author dturner@kixeye.com
 */
public class LoggerConfigurationWriter implements ConfigurationWriter{

    private Logger logger;

    public LoggerConfigurationWriter(Logger logger){
        this.logger = logger;
    }

    @Override
    public void write(Configuration configuration, Filter filter) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Configuring service with configuration properties:");
        Iterator<String> keys = configuration.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            sb.append("\n    ").append(key).append("=").append(configuration.getProperty(key));
        }
        logger.debug(sb.toString());
    }

}
