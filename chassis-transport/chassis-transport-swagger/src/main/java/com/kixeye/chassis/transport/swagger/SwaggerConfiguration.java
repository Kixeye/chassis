package com.kixeye.chassis.transport.swagger;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.mangofactory.swagger.configuration.DocumentationConfig;

/**
 * Configures Swagger.
 * 
 * @author ebahtijaragic
 */
@Configuration
@Import({DocumentationConfig.class})
@ComponentScan(basePackageClasses=SwaggerConfiguration.class)
public class SwaggerConfiguration {

}
