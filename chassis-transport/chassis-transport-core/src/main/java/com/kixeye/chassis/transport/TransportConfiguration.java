package com.kixeye.chassis.transport;

/*
 * #%L
 * Chassis Transport Core
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
