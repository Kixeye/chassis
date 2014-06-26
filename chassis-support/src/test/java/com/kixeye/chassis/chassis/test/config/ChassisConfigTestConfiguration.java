package com.kixeye.chassis.chassis.test.config;

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
