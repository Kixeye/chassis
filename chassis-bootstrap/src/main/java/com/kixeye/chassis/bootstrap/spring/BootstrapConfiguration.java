package com.kixeye.chassis.bootstrap.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Spring configuration added to the Spring context when the client clientApplication is starting a Spring app. Beans created by this
 * configuration will be added to the client clientApplication's context and will be accessible to the client clientApplication.
 *
 * @author dturner@kixeye.com
 */
@Configuration
public class BootstrapConfiguration {

    /**
     * Create a  PropertyPlaceholderConfigurer that resolves environment property sources.
     *
     * @return PropertySourcesPlaceholderConfigurer
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
