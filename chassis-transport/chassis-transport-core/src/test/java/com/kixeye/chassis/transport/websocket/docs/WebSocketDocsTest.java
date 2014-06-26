package com.kixeye.chassis.transport.websocket.docs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.kixeye.chassis.transport.TransportConfiguration;
import com.kixeye.chassis.transport.utils.SocketUtils;

/**
 * Tests the web-socket documentation generation.
 * 
 * @author ebahtijaragic
 */
public class WebSocketDocsTest {
	private static final Logger logger = LoggerFactory.getLogger(WebSocketDocsTest.class);
	
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
	public void testProtobufMessagesSchema() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("admin.enabled", "true");
		properties.put("admin.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("admin.hostname", "localhost");
		
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);

		RestTemplate httpClient = new RestTemplate();
		
		try {
			context.refresh();

			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));

			ResponseEntity<String> response = httpClient.getForEntity(
					new URI("http://localhost:" + properties.get("admin.port") + "/schema/messages/protobuf"), String.class);
			
			logger.info("Got response: [{}]", response);
			
			Assert.assertEquals(response.getStatusCode().value(), HttpStatus.OK.value());
			Assert.assertTrue(response.getBody().contains("message error"));
		} finally {
			context.close();
		}
	}
	
	@Test
	public void testProtobufMessagesEnvelope() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("admin.enabled", "true");
		properties.put("admin.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("admin.hostname", "localhost");
		
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");

		properties.put("http.enabled", "false");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);

		RestTemplate httpClient = new RestTemplate();
		
		try {
			context.refresh();

			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));

			ResponseEntity<String> response = httpClient.getForEntity(
					new URI("http://localhost:" + properties.get("admin.port") + "/schema/envelope/protobuf"), String.class);
			
			logger.info("Got response: [{}]", response);
			
			Assert.assertEquals(response.getStatusCode().value(), HttpStatus.OK.value());
			Assert.assertTrue(response.getBody().contains("message Envelope"));
		} finally {
			context.close();
		}
	}
}
