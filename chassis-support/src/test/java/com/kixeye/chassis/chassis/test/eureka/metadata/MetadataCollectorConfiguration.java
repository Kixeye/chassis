package com.kixeye.chassis.chassis.test.eureka.metadata;

import com.kixeye.chassis.chassis.eureka.EurekaConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ComponentScan(basePackageClasses = MetadataCollectorConfiguration.class)
@Import({EurekaConfiguration.class})
public class MetadataCollectorConfiguration {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}