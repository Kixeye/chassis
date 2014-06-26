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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.HexDump;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.kixeye.chassis.transport.http.SerDeHttpMessageConverter;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.utils.SocketUtils;

/**
 * Tests the metrics configs.
 * 
 * @author ebahtijaragic
 */
public class MetricsTest {
	private static final Logger logger = LoggerFactory.getLogger(MetricsTest.class);
	
	public static final ClientHttpRequestInterceptor LOGGING_INTERCEPTOR = new ClientHttpRequestInterceptor() {
		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			if (body.length > 0) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				HexDump.dump(body, 0, baos, 0);
				
				logger.info("Sending to [{}]: \n{}", request.getURI(), baos.toString(Charsets.UTF_8.name()).trim());
			} else {
				logger.info("Sending empty body to [{}]!", request.getURI());
			}
			
			return execution.execute(request, body);
		}
	};
	
	@Test
	public void testMetricsPing() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("admin.enabled", "true");
		properties.put("admin.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("admin.hostname", "localhost");
		
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "true");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(MetricsConfiguration.class);

		RestTemplate httpClient = new RestTemplate();
		
		try {
			context.refresh();

			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
			for (MessageSerDe serDe : context.getBeansOfType(MessageSerDe.class).values()) {
				messageConverters.add(new SerDeHttpMessageConverter(serDe));
			}
			messageConverters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
			httpClient.setMessageConverters(messageConverters);

			ResponseEntity<String> response = httpClient.getForEntity(new URI("http://localhost:" + properties.get("admin.port") + "/metrics/ping"), String.class);
			
			logger.info("Got response: [{}]", response);
			
			Assert.assertEquals(response.getStatusCode().value(), HttpStatus.OK.value());
			Assert.assertEquals("pong".trim(), response.getBody().trim());
		} finally {
			context.close();
		}
	}
	
}
