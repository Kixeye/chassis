package com.kixeye.chassis.transport;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import com.kixeye.chassis.transport.admin.AdminTransportConfiguration;
import com.kixeye.chassis.transport.http.HttpTransportConfiguration;
import com.kixeye.chassis.transport.serde.SerDeConfiguration;
import com.kixeye.chassis.transport.websocket.WebSocketTransportConfiguration;

/**
 * Configures the transport.
 * 
 * @author ebahtijaragic
 */
@Configuration
@Import({ AdminTransportConfiguration.class, HttpTransportConfiguration.class, WebSocketTransportConfiguration.class, SerDeConfiguration.class})
@PropertySource("classpath:/transport-default.properties")
public class TransportConfiguration {
	@Bean
	public Validator messageValidator() {
		// force hibernate validator to log to slf4j
		System.setProperty("org.jboss.logging.provider", "slf4j");
		
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		
	    return factory.getValidator();
	}
}
