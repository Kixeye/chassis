package com.kixeye.chassis.support.test.config;

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

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;

@Configuration
public class ChassisConfigTestConfiguration {

    @Bean
    static public PropertyPlaceholderConfigurer archaiusPropertyPlaceholderConfigurer() throws IOException, ConfigurationException {
    	
        ConfigurationManager.loadPropertiesFromResources("chassis-test.properties");
        ConfigurationManager.loadPropertiesFromResources("chassis-default.properties");
        
        // force disable eureka
        ConfigurationManager.getConfigInstance().setProperty("chassis.eureka.disable", true);
        
        return new PropertyPlaceholderConfigurer() {
            @Override
            protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
                return DynamicPropertyFactory.getInstance().getStringProperty(placeholder, "null").get();
            }
        };
    }
}
