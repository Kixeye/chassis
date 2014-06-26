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
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.HexDump;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hibernate.validator.constraints.Length;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import rx.Observable;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.kixeye.chassis.transport.dto.ServiceError;
import com.kixeye.chassis.transport.http.HttpServiceException;
import com.kixeye.chassis.transport.http.SerDeHttpMessageConverter;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.serde.converter.JsonMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.ProtobufMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.XmlMessageSerDe;
import com.kixeye.chassis.transport.serde.converter.YamlMessageSerDe;
import com.kixeye.chassis.transport.utils.SocketUtils;

/**
 * Tests the HTTP transport.
 * 
 * @author ebahtijaragic
 */
public class HttpTransportTest {
	private static final Logger logger = LoggerFactory.getLogger(HttpTransportTest.class);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

	public static final ClientHttpRequestInterceptor LOGGING_INTERCEPTOR = new ClientHttpRequestInterceptor() {
		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			logger.info("Sending headers: " + request.getHeaders());
			
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
	public void testHttpServiceWithJson() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("http.enabled", "true");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		properties.put("websocket.enabled", "false");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestRestService.class);

		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(JsonMessageSerDe.class);

			RestTemplate httpClient = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
			httpClient.setErrorHandler(new ResponseErrorHandler() {
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return response.getRawStatusCode() == HttpStatus.OK.value();
				}
				
				public void handleError(ClientHttpResponse response) throws IOException {
					
				}
			});
			
			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));
			httpClient.setMessageConverters(new ArrayList<HttpMessageConverter<?>>(Lists.newArrayList(new SerDeHttpMessageConverter(serDe))));
			
			TestObject response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			response = httpClient.postForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject("more stuff"),
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);
			
			response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);

            response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/getFuture"),
                    TestObject.class);

            Assert.assertNotNull(response);
            Assert.assertEquals("more stuff", response.value);

            response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/getObservable"),
                    TestObject.class);

            Assert.assertNotNull(response);
            Assert.assertEquals("more stuff", response.value);


            ResponseEntity <ServiceError> error = httpClient.postForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"),
					new TestObject(RandomStringUtils.randomAlphabetic(100)),
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.getBody().code);
			
			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/expectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION_HTTP_CODE, error.getStatusCode());
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.code, error.getBody().code);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.description, error.getBody().description);

			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/unexpectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.getBody().code);
		} finally {
			context.close();
		}
	}

	@Test
	public void testHttpServiceWithJsonWithHTTPS() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		
		properties.put("https.enabled", "true");
		properties.put("https.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("https.hostname", "localhost");
		properties.put("https.selfSigned", "true");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestRestService.class);

		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(JsonMessageSerDe.class);
			
			SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    return true;
                }
            });
            SSLContext sslContext = builder.build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext, new X509HostnameVerifier() {
                        @Override
                        public void verify(String host, SSLSocket ssl)
                                throws IOException {
                        }

                        @Override
                        public void verify(String host, X509Certificate cert)
                                throws SSLException {
                        }

                        @Override
                        public void verify(String host, String[] cns,
                                String[] subjectAlts) throws SSLException {
                        }

                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return true;
                        }
                    });

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                    .<ConnectionSocketFactory> create().register("https", sslsf)
                    .build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(
                    socketFactoryRegistry);
            
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(HttpClients.custom().setConnectionManager(cm).build());

            RestTemplate httpClient = new RestTemplate(requestFactory);
			httpClient.setErrorHandler(new ResponseErrorHandler() {
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return response.getRawStatusCode() == HttpStatus.OK.value();
				}
				
				public void handleError(ClientHttpResponse response) throws IOException {
					
				}
			});
			
			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));
			httpClient.setMessageConverters(new ArrayList<HttpMessageConverter<?>>(Lists.newArrayList(new SerDeHttpMessageConverter(serDe))));
			
			TestObject response = httpClient.getForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			response = httpClient.postForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/"), 
					new TestObject("more stuff"),
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);
			
			response = httpClient.getForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);

            response = httpClient.getForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/getFuture"),
                    TestObject.class);

            Assert.assertNotNull(response);
            Assert.assertEquals("more stuff", response.value);

            response = httpClient.getForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/getObservable"),
                    TestObject.class);

            Assert.assertNotNull(response);
            Assert.assertEquals("more stuff", response.value);


            ResponseEntity <ServiceError> error = httpClient.postForEntity(new URI("https://localhost:" + properties.get("https.port") + "/stuff/"),
					new TestObject(RandomStringUtils.randomAlphabetic(100)),
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.getBody().code);
			
			error = httpClient.getForEntity(new URI("https://localhost:" + properties.get("https.port") + "/stuff/expectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION_HTTP_CODE, error.getStatusCode());
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.code, error.getBody().code);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.description, error.getBody().description);

			error = httpClient.getForEntity(new URI("https://localhost:" + properties.get("https.port") + "/stuff/unexpectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.getBody().code);
		} finally {
			context.close();
		}
	}
	
	@Test
	public void testHttpServiceWithJsonWithHTTPSAndHTTP() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();

		properties.put("http.enabled", "true");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		properties.put("https.enabled", "true");
		properties.put("https.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("https.hostname", "localhost");
		properties.put("https.selfSigned", "true");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestRestService.class);

		try {
			context.refresh();

			final MessageSerDe serDe = context.getBean(JsonMessageSerDe.class);
			
			SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    return true;
                }
            });
            SSLContext sslContext = builder.build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext, new X509HostnameVerifier() {
                        @Override
                        public void verify(String host, SSLSocket ssl)
                                throws IOException {
                        }

                        @Override
                        public void verify(String host, X509Certificate cert)
                                throws SSLException {
                        }

                        @Override
                        public void verify(String host, String[] cns,
                                String[] subjectAlts) throws SSLException {
                        }

                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return true;
                        }
                    });

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                    .<ConnectionSocketFactory> create().register("https", sslsf).register("http", new PlainConnectionSocketFactory())
                    .build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(
                    socketFactoryRegistry);
            
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(HttpClients.custom().setConnectionManager(cm).build());

            RestTemplate httpClient = new RestTemplate(requestFactory);
			httpClient.setErrorHandler(new ResponseErrorHandler() {
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return response.getRawStatusCode() == HttpStatus.OK.value();
				}
				
				public void handleError(ClientHttpResponse response) throws IOException {
					
				}
			});
			
			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));
			httpClient.setMessageConverters(new ArrayList<HttpMessageConverter<?>>(Lists.newArrayList(new SerDeHttpMessageConverter(serDe))));
			
			TestObject response = httpClient.getForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			response = httpClient.postForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/"), 
					new TestObject("more stuff"),
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);
			
			response = httpClient.getForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);

            response = httpClient.getForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/getFuture"),
                    TestObject.class);

            Assert.assertNotNull(response);
            Assert.assertEquals("more stuff", response.value);

            response = httpClient.getForObject(new URI("https://localhost:" + properties.get("https.port") + "/stuff/getObservable"),
                    TestObject.class);

            Assert.assertNotNull(response);
            Assert.assertEquals("more stuff", response.value);

            ResponseEntity <ServiceError> error = httpClient.postForEntity(new URI("https://localhost:" + properties.get("https.port") + "/stuff/"),
					new TestObject(RandomStringUtils.randomAlphabetic(100)),
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.getBody().code);
			
			error = httpClient.getForEntity(new URI("https://localhost:" + properties.get("https.port") + "/stuff/expectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION_HTTP_CODE, error.getStatusCode());
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.code, error.getBody().code);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.description, error.getBody().description);

			error = httpClient.getForEntity(new URI("https://localhost:" + properties.get("https.port") + "/stuff/unexpectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.getBody().code);
			
			response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);

			response = httpClient.postForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject("stuff"),
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

            response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/getFuture"),
                    TestObject.class);

            Assert.assertNotNull(response);
            Assert.assertEquals("stuff", response.value);

            response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/getObservable"),
                    TestObject.class);

            Assert.assertNotNull(response);
            Assert.assertEquals("stuff", response.value);

            error = httpClient.postForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"),
					new TestObject(RandomStringUtils.randomAlphabetic(100)),
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.getBody().code);
			
			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/expectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION_HTTP_CODE, error.getStatusCode());
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.code, error.getBody().code);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.description, error.getBody().description);

			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/unexpectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.getBody().code);
		} finally {
			context.close();
		}
	}

    @Test
	public void testHttpServiceWithProtobuf() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("http.enabled", "true");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		properties.put("websocket.enabled", "true");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestRestService.class);

		try {
			context.refresh();
			
			final MessageSerDe serDe = context.getBean(ProtobufMessageSerDe.class);

			RestTemplate httpClient = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
			httpClient.setErrorHandler(new ResponseErrorHandler() {
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return response.getRawStatusCode() == HttpStatus.OK.value();
				}
				
				public void handleError(ClientHttpResponse response) throws IOException {
					
				}
			});
			
			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));
			httpClient.setMessageConverters(new ArrayList<HttpMessageConverter<?>>(Lists.newArrayList(new SerDeHttpMessageConverter(serDe))));
			
			TestObject response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			response = httpClient.postForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject("more stuff"),
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);
			
			response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			ResponseEntity<ServiceError> error = httpClient.postForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject(RandomStringUtils.randomAlphabetic(100)),
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.getBody().code);
			
			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/expectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION_HTTP_CODE, error.getStatusCode());
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.code, error.getBody().code);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.description, error.getBody().description);

			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/unexpectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.getBody().code);

			error = httpClient.postForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/headerRequired"), 
					null, ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.getBody().code);
		} finally {
			context.close();
		}
	}
	
	@Test
	public void testHttpServiceWithXml() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("http.enabled", "true");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		properties.put("websocket.enabled", "false");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestRestService.class);

		try {
			context.refresh();
			
			final MessageSerDe serDe = context.getBean(XmlMessageSerDe.class);

			RestTemplate httpClient = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
			httpClient.setErrorHandler(new ResponseErrorHandler() {
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return response.getRawStatusCode() == HttpStatus.OK.value();
				}
				
				public void handleError(ClientHttpResponse response) throws IOException {
					
				}
			});
			
			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));
			httpClient.setMessageConverters(new ArrayList<HttpMessageConverter<?>>(Lists.newArrayList(new SerDeHttpMessageConverter(serDe))));
			
			TestObject response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			response = httpClient.postForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject("more stuff"),
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);
			
			response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			ResponseEntity<ServiceError> error = httpClient.postForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject(RandomStringUtils.randomAlphabetic(100)),
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.getBody().code);
			
			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/expectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION_HTTP_CODE, error.getStatusCode());
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.code, error.getBody().code);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.description, error.getBody().description);

			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/unexpectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.getBody().code);
		} finally {
			context.close();
		}
	}
	
	@Test
	public void testHttpServiceWithYaml() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("http.enabled", "true");
		properties.put("http.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("http.hostname", "localhost");
		
		properties.put("websocket.enabled", "false");
		properties.put("websocket.port", "" + SocketUtils.findAvailableTcpPort());
		properties.put("websocket.hostname", "localhost");
		
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("default", properties));
		context.setEnvironment(environment);
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(TransportConfiguration.class);
		context.register(TestRestService.class);

		try {
			context.refresh();
			
			final MessageSerDe serDe = context.getBean(YamlMessageSerDe.class);

			RestTemplate httpClient = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
			httpClient.setErrorHandler(new ResponseErrorHandler() {
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return response.getRawStatusCode() == HttpStatus.OK.value();
				}
				
				public void handleError(ClientHttpResponse response) throws IOException {
					
				}
			});
			
			httpClient.setInterceptors(Lists.newArrayList(LOGGING_INTERCEPTOR));
			httpClient.setMessageConverters(new ArrayList<HttpMessageConverter<?>>(Lists.newArrayList(new SerDeHttpMessageConverter(serDe))));
			
			TestObject response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);

			response = httpClient.postForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject("more stuff"),
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("stuff", response.value);
			
			response = httpClient.getForObject(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					TestObject.class);
			
			Assert.assertNotNull(response);
			Assert.assertEquals("more stuff", response.value);
			
			ResponseEntity<ServiceError> error = httpClient.postForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/"), 
					new TestObject(RandomStringUtils.randomAlphabetic(100)),
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.VALIDATION_ERROR_CODE, error.getBody().code);
			
			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/expectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION_HTTP_CODE, error.getStatusCode());
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.code, error.getBody().code);
			Assert.assertEquals(TestRestService.EXPECTED_EXCEPTION.description, error.getBody().description);

			error = httpClient.getForEntity(new URI("http://localhost:" + properties.get("http.port") + "/stuff/unexpectedError"), 
					ServiceError.class);

			Assert.assertNotNull(response);
			Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
			Assert.assertEquals(ExceptionServiceErrorMapper.UNKNOWN_ERROR_CODE, error.getBody().code);
		} finally {
			context.close();
		}
	}
	
	@RestController
	@RequestMapping("/stuff")
	public static class TestRestService {
		public static final ServiceError EXPECTED_EXCEPTION = new ServiceError("expected", "Expected exception!");
		public static final HttpStatus EXPECTED_EXCEPTION_HTTP_CODE = HttpStatus.SERVICE_UNAVAILABLE;
		
		public final AtomicReference<TestObject> stuff = new AtomicReference<>(new TestObject("stuff"));

		@RequestMapping(method={ RequestMethod.POST }, value="/headerRequired")
		public void headerRequired(@RequestHeader(value = "Some-Required-Header") final String requiredHeader) {
			throw new HttpServiceException(EXPECTED_EXCEPTION, EXPECTED_EXCEPTION_HTTP_CODE.value());
		}

		@RequestMapping(method={ RequestMethod.GET }, value="/expectedError")
		public void throwExpectedException() {
			throw new HttpServiceException(EXPECTED_EXCEPTION, EXPECTED_EXCEPTION_HTTP_CODE.value());
		}

		@RequestMapping(method={ RequestMethod.GET }, value="/unexpectedError")
		public void throwUnexpectedException() {
			throw new RuntimeException("unexpected");
		}
		
		@RequestMapping(method={ RequestMethod.GET }, value="/")
		public DeferredResult<TestObject> getStuff() {
			DeferredResult<TestObject> result = new DeferredResult<TestObject>();
			result.setResult(stuff.get());
			
			return result;
		}

        @RequestMapping(method={ RequestMethod.GET }, value="/getFuture")
        public ListenableFuture<TestObject> getStuffListenableFuture() {
            SettableFuture<TestObject> future = SettableFuture.create();
            future.set(stuff.get());
            return future;
        }

        @RequestMapping(method={ RequestMethod.GET }, value="/getObservable")
        public Observable<TestObject> getStuffListenableObservable() {
            Observable<TestObject> observable = Observable.just( stuff.get() );
            return observable;
        }
		
		@RequestMapping(method={ RequestMethod.POST }, value="/")
		public DeferredResult<TestObject> setStuff(@RequestBody @Valid TestObject request) {
			DeferredResult<TestObject> result = new DeferredResult<TestObject>();
			result.setResult(stuff.get());
			
			stuff.set(request);
			
			return result;
		}
	}

	public static class TestObject {
		@NotNull
		@Length(max=50)
		public String value = null;
		
		public TestObject(String value) {
			this.value = value;
		}
		
		public TestObject() {
			
		}
	}
}
