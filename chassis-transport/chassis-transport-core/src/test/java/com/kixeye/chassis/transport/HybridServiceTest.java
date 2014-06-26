package com.kixeye.chassis.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.HexDump;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.kixeye.chassis.transport.dto.Envelope;
import com.kixeye.chassis.transport.http.SerDeHttpMessageConverter;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.serde.converter.ProtobufMessageSerDe;
import com.kixeye.chassis.transport.utils.SocketUtils;
import com.kixeye.chassis.transport.websocket.ActionMapping;
import com.kixeye.chassis.transport.websocket.ActionPayload;
import com.kixeye.chassis.transport.websocket.QueuingWebSocketListener;
import com.kixeye.chassis.transport.websocket.WebSocketController;
import com.kixeye.chassis.transport.websocket.WebSocketMessageRegistry;

/**
 * Tests the Hybrid service support.
 * 
 * @author ebahtijaragic
 */
public class HybridServiceTest {
	private static final Logger logger = LoggerFactory.getLogger(HybridServiceTest.class);
	
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
	public void testHybridService() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
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
		context.register(TestCombinedService.class);

		WebSocketClient wsClient = new WebSocketClient();

		RestTemplate httpClient = new RestTemplate();
		
		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(ProtobufMessageSerDe.class);

			final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);
			
			messageRegistry.registerType("stuff", TestObject.class);
			
			wsClient.start();

			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
			for (MessageSerDe messageSerDe : context.getBeansOfType(MessageSerDe.class).values()) {
				messageConverters.add(new SerDeHttpMessageConverter(messageSerDe));
			}
			messageConverters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
			httpClient.setMessageConverters(messageConverters);
			
			QueuingWebSocketListener webSocket = new QueuingWebSocketListener(serDe, messageRegistry, null);

			Session session = wsClient.connect(webSocket, new URI("ws://localhost:" +  properties.get("websocket.port") + "/protobuf")).get(5000, TimeUnit.MILLISECONDS);

			Envelope envelope = new Envelope("getStuff", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			TestObject response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			byte[] rawStuff = serDe.serialize(new TestObject("more stuff"));
			
			envelope = new Envelope("setStuff", "stuff", null, ByteBuffer.wrap(rawStuff));
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			envelope = new Envelope("getStuff", null, null, null);
			
			session.getRemote().sendBytes(ByteBuffer.wrap(serDe.serialize(envelope)));
			
			response = webSocket.getResponse(5, TimeUnit.SECONDS);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);

			response = httpClient.postForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject("even more stuff"),
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("even more stuff", response.value);
		} finally {
			try {
				wsClient.stop();
			} finally {
				context.close();
			}
		}
	}
	
	@RestController
	@WebSocketController
	@RequestMapping("/stuff")
	public static class TestCombinedService {
		public final AtomicReference<TestObject> stuff = new AtomicReference<>(new TestObject("stuff"));
		
		@ActionMapping("getStuff")
		@RequestMapping(method={ RequestMethod.GET }, value="/")
		public DeferredResult<TestObject> getStuff() {
			DeferredResult<TestObject> result = new DeferredResult<TestObject>();
			result.setResult(stuff.get());
			
			return result;
		}
		
		@ActionMapping("setStuff")
		@RequestMapping(method={ RequestMethod.POST }, value="/")
		public DeferredResult<TestObject> setStuff(@RequestBody @ActionPayload TestObject request) {
			DeferredResult<TestObject> result = new DeferredResult<TestObject>();
			result.setResult(stuff.get());
			
			stuff.set(request);
			
			return result;
		}
	}
	
	public static class TestObject {
		public String value = null;
		
		public TestObject(String value) {
			this.value = value;
		}
		
		public TestObject() {
			
		}
	}
}
