package com.kixeye.chassis.support.test.eureka;

/*
 * #%L
 * Chassis Support
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

@Configuration
@ComponentScan(basePackageClasses=ChassisEurekaTestConfiguration.class)
public class ChassisEurekaTestConfiguration extends DelegatingWebMvcConfiguration {

    @Bean
    static public PropertyPlaceholderConfigurer archaiusPropertyPlaceholderConfigurer() throws IOException, ConfigurationException {
    	
        ConfigurationManager.loadPropertiesFromResources("chassis-test.properties");
        ConfigurationManager.loadPropertiesFromResources("chassis-default.properties");
        
        return new PropertyPlaceholderConfigurer() {
            @Override
            protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
                return DynamicPropertyFactory.getInstance().getStringProperty(placeholder, "null").get();
            }
        };
    }

    @Order(0)
    @Bean(initMethod="start", destroyMethod="stop")
    public Server httpServer(
            @Value("${http.hostname}") String hostname,
            @Value("${http.port}") int port,
            ConfigurableWebApplicationContext webApplicationContext) {

        // set up servlets
        ServletHandler servlets = new ServletHandler();
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.setErrorHandler(null);
        context.setWelcomeFiles(new String[] { "/" });

        // set up spring with the servlet context
        setServletContext(context.getServletContext());

        // configure the spring mvc dispatcher
        DispatcherServlet dispatcher = new DispatcherServlet(webApplicationContext);

        // map application servlets
        context.addServlet(new ServletHolder(dispatcher), "/");

        servlets.setHandler(context);

        // create the server
        InetSocketAddress address = StringUtils.isBlank(hostname) ? new InetSocketAddress(port) : new InetSocketAddress(hostname, port);
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(address.getHostName());
        connector.setPort(address.getPort());
        server.setConnectors(new Connector[]{ connector });
        server.setHandler(servlets);

        return server;
    }
}
