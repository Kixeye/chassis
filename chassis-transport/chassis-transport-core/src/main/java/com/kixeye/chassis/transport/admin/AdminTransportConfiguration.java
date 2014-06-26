package com.kixeye.chassis.transport.admin;

import java.net.InetSocketAddress;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.kixeye.chassis.transport.admin.AdminTransportConfiguration.AdminEnabledCondition;
import com.kixeye.chassis.transport.shared.ClasspathDumpServlet;
import com.kixeye.chassis.transport.shared.ClasspathResourceServlet;
import com.kixeye.chassis.transport.shared.HealthServlet;
import com.kixeye.chassis.transport.shared.JettyConnectorRegistry;
import com.kixeye.chassis.transport.shared.PropertiesServlet;
import com.kixeye.chassis.transport.websocket.WebSocketMessageMappingRegistry;
import com.kixeye.chassis.transport.websocket.WebSocketMessageRegistry;
import com.kixeye.chassis.transport.websocket.docs.ProtobufEnvelopeDocumentationServlet;
import com.kixeye.chassis.transport.websocket.docs.ProtobufMessagesDocumentationServlet;

/**
 * Configures the Http transport.
 * 
 * @author ebahtijaragic
 */
@Configuration
@Conditional(AdminEnabledCondition.class)
@ComponentScan(basePackageClasses=AdminTransportConfiguration.class)
public class AdminTransportConfiguration {
	
	@Value("${app.name:UNKNOWN}")
	private String appName;

	@Autowired(required = false)
	private WebSocketMessageRegistry messageRegistry;
	
	@Autowired(required = false)
	private WebSocketMessageMappingRegistry mappingRegistry;
	
    @Autowired(required = false)
    private MetricRegistry metricRegistry;

    @Autowired(required = false)
    private HealthCheckRegistry healthCheckRegistry;

    @Value("${http.metrics.threadpool.enabled}")
    private boolean monitorThreadpool;

    @Value("${http.metrics.handler.enabled}")
    private boolean monitorHandler;

    @Bean(initMethod="start", destroyMethod="stop")
	@Order(0)
	public Server adminServer(
            @Value("${admin.hostname}") String hostname,
            @Value("${admin.port}") int port) {

        // set up servlets
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
		context.setErrorHandler(null);
		context.setWelcomeFiles(new String[] { "/" });
		
		// enable gzip
		context.addFilter(GzipFilter.class, "/*", null);
		
		// add common admin servlets
		context.addServlet(new ServletHolder(new HealthServlet(healthCheckRegistry)), "/healthcheck");
		context.addServlet(new ServletHolder(new ClasspathResourceServlet("com/kixeye/chassis/transport/admin")), "/admin/*");
		context.addServlet(new ServletHolder(new PropertiesServlet()), "/admin/properties");
		context.addServlet(new ServletHolder(new ClasspathDumpServlet()), "/admin/classpath");

        // add websocket servlets if WebSockets have been initialized
        if (mappingRegistry != null && messageRegistry != null) {
            context.addServlet(new ServletHolder(new ProtobufMessagesDocumentationServlet(appName, mappingRegistry, messageRegistry)), "/schema/messages/protobuf");
            context.addServlet(new ServletHolder(new ProtobufEnvelopeDocumentationServlet()), "/schema/envelope/protobuf");
        }
		
        // add metric servlets if Metric has been initialized
        if (metricRegistry != null && healthCheckRegistry != null) {
            context.getServletContext().setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
            context.getServletContext().setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
            
            ServletHolder holder = new ServletHolder(new AdminServlet());
            holder.setInitParameter("service-name", System.getProperty("app.name"));
            context.addServlet(holder, "/metrics/*");
        }

        // create the server
    	InetSocketAddress address = StringUtils.isBlank(hostname) ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port);
    	
    	Server server = new Server();

    	JettyConnectorRegistry.registerHttpConnector(server, address);
        server.setHandler(context);
    
		return server;
	}

    /**
	 * A condition to check whether HTTP is enabled.
	 * 
	 * @author ebahtijaragic
	 */
	public static class AdminEnabledCondition implements Condition {
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return StringUtils.equalsIgnoreCase(context.getEnvironment().resolvePlaceholders("${admin.enabled}"), "true");
		}
	}
}
