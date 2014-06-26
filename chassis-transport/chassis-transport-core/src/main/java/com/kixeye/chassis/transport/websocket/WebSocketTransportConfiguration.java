package com.kixeye.chassis.transport.websocket;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jetty9.InstrumentedQueuedThreadPool;
import com.kixeye.chassis.transport.http.InstrumentedHandler;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.shared.JettyConnectorRegistry;
import com.kixeye.chassis.transport.websocket.WebSocketTransportConfiguration.WebSocketEnabledCondition;
import com.netflix.config.ConfigurationManager;

/**
 * Configures the WebSocket transport.
 * 
 * @author ebahtijaragic
 */
@Configuration
@Conditional(WebSocketEnabledCondition.class)
@ComponentScan(basePackageClasses=WebSocketTransportConfiguration.class)
public class WebSocketTransportConfiguration {

	@Value("${app.name:UNKNOWN}")
	private String appName;

	@Autowired
	private WebSocketMessageRegistry messageRegistry;
	
	@Autowired
	private WebSocketMessageMappingRegistry mappingRegistry;
	
    @Autowired(required = false)
    private MetricRegistry metricRegistry;

    @Autowired(required = false)
    private HealthCheckRegistry healthCheckRegistry;

    @Value("${websocket.metrics.threadpool.enabled}")
    private boolean monitorThreadpool;

    @Value("${websocket.metrics.handler.enabled}")
    private boolean monitorHandler;

    @Autowired
    private Set<MessageSerDe> serDes;

	@Bean(initMethod="start", destroyMethod="stop")
	@Order(0)
	public Server webSocketServer(
			@Value("${websocket.enabled:false}") boolean websocketEnabled,
            @Value("${websocket.hostname:}") String websocketHostname,
            @Value("${websocket.port:-1}") int websocketPort,
            
			@Value("${secureWebsocket.enabled:false}") boolean secureWebsocketEnabled,
            @Value("${secureWebsocket.hostname:}") String secureWebsocketHostname,
            @Value("${secureWebsocket.port:-1}") int secureWebsocketPort,
            @Value("${secureWebsocket.selfSigned:false}") boolean selfSigned,
            @Value("${secureWebsocket.mutualSsl:false}") boolean mutualSsl,

			@Value("${secureWebsocket.keyStorePath:}") String keyStorePath,
			@Value("${secureWebsocket.keyStoreData:}") String keyStoreData,
			@Value("${secureWebsocket.keyStorePassword:}") String keyStorePassword,
			@Value("${secureWebsocket.keyManagerPassword:}") String keyManagerPassword,

			@Value("${secureWebsocket.trustStorePath:}") String trustStorePath,
			@Value("${secureWebsocket.trustStoreData:}") String trustStoreData,
			@Value("${secureWebsocket.trustStorePassword:}") String trustStorePassword,
			
			@Value("${securewebsocket.excludedCipherSuites:}") String[] excludedCipherSuites) throws Exception {
		// set up servlets
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
		context.setErrorHandler(null);
		context.setWelcomeFiles(new String[] { "/" });
		
		for (final MessageSerDe serDe : serDes) {
			// create the websocket creator
			final WebSocketCreator webSocketCreator = new WebSocketCreator() {
				public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
					// this will have spring construct a new one for every session
					ActionInvokingWebSocket webSocket = forwardingWebSocket();
					webSocket.setSerDe(serDe);
					webSocket.setUpgradeRequest(req);
					webSocket.setUpgradeResponse(resp);
					
					return webSocket;
				}
			};
			
			// configure the websocket servlet
			ServletHolder webSocketServlet = new ServletHolder(new WebSocketServlet() {
				private static final long serialVersionUID = -3022799271546369505L;
	
				@Override
				public void configure(WebSocketServletFactory factory) {
					factory.setCreator(webSocketCreator);
				}
			});
			
			Map<String, String> webSocketProperties = new HashMap<>();
			AbstractConfiguration config = ConfigurationManager.getConfigInstance();
			Iterator<String> webSocketPropertyKeys = config.getKeys("websocket");
			while (webSocketPropertyKeys.hasNext()) {
				String key = webSocketPropertyKeys.next();
				
				webSocketProperties.put(key.replaceFirst(Pattern.quote("websocket."), ""), config.getString(key));
			}
			
			webSocketServlet.setInitParameters(webSocketProperties);
			
			context.addServlet(webSocketServlet,  "/" + serDe.getMessageFormatName() + "/*");
		}

		// create the server
    	Server server;
    	if (metricRegistry == null || !monitorThreadpool){
        	server = new Server();

            server.setHandler(context);
        } else {
        	server = new Server(new InstrumentedQueuedThreadPool(metricRegistry));
        	
        	InstrumentedHandler instrumented = new InstrumentedHandler(metricRegistry);
            instrumented.setHandler(context);

            server.setHandler(instrumented);
        }
        
    	// set up connectors
    	if (websocketEnabled) {
        	InetSocketAddress address = StringUtils.isBlank(websocketHostname) ? new InetSocketAddress(websocketPort) : new InetSocketAddress(websocketHostname, websocketPort);
        	
        	JettyConnectorRegistry.registerHttpConnector(server, address);
    	}
    	
    	if (secureWebsocketEnabled) {
        	InetSocketAddress address = StringUtils.isBlank(secureWebsocketHostname) ? new InetSocketAddress(secureWebsocketPort) : new InetSocketAddress(secureWebsocketHostname, secureWebsocketPort);
        	
        	JettyConnectorRegistry.registerHttpsConnector(server, address, selfSigned, mutualSsl, keyStorePath, keyStoreData, keyStorePassword, keyManagerPassword, 
        			trustStorePath, trustStoreData, trustStorePassword, excludedCipherSuites);
    	}
        
		return server;
	}
	
	@Bean
	@Scope("prototype")
	public ActionInvokingWebSocket forwardingWebSocket() {
		return new ActionInvokingWebSocket();
	}
	
	/**
	 * A condition to check whether HTTP is enabled.
	 * 
	 * @author ebahtijaragic
	 */
	public static class WebSocketEnabledCondition implements Condition {
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return StringUtils.equalsIgnoreCase(context.getEnvironment().resolvePlaceholders("${websocket.enabled}"), "true") || StringUtils.equalsIgnoreCase(context.getEnvironment().resolvePlaceholders("${secureWebsocket.enabled}"), "true");
		}
	}
}
