package com.kixeye.chassis.bootstrap.webapp;

/*
 * #%L
 * Chassis Bootstrap
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

import com.kixeye.chassis.bootstrap.annotation.App;
import com.kixeye.chassis.bootstrap.annotation.Destroy;
import com.kixeye.chassis.bootstrap.annotation.Init;
import com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.SocketUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Spring web app configuration
 *
 * @author dturner@kixeye.com
 */
@App(name = WebAppIntegrationTest.UNIT_TEST_SPRING_APP, configurationClasses = TestSpringWebApp.class, webapp = true)
@Configuration
@ComponentScan(basePackageClasses = TestSpringWebApp.class)
public class TestSpringWebApp extends DelegatingWebMvcConfiguration {

    public static Queue<String> eventQueue = new LinkedBlockingDeque<>();

    public static void reset() {
        eventQueue.clear();
    }

    @Init
    public static void init(org.apache.commons.configuration.Configuration configuration, ConfigurationProvider configurationProvider) {
        eventQueue.add("init");
    }

    @Destroy
    public static void destroy() {
        eventQueue.add("destroy");
    }

    @PostConstruct
    public void postConstruct() {
        eventQueue.add("postConstruct");
    }

    @PreDestroy
    public void preDestroy() {
        eventQueue.add("preDestroy");
    }

    @Bean(initMethod = "start", destroyMethod = "stop", name = "httpServer")
    @Order(0)
    public Server httpServer(ConfigurableWebApplicationContext webApplicationContext) {

        // set up servlets
        ServletHandler servlets = new ServletHandler();
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SECURITY);
        context.setErrorHandler(null);
        context.setWelcomeFiles(new String[]{"/"});

        // set up spring with the servlet context
        setServletContext(context.getServletContext());

        // configure the spring mvc dispatcher
        DispatcherServlet dispatcher = new DispatcherServlet(webApplicationContext);

        // map application servlets
        context.addServlet(new ServletHolder(dispatcher), "/");

        servlets.setHandler(context);

        // create the server
        InetSocketAddress address = new InetSocketAddress(SocketUtils.findAvailableTcpPort());

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(address.getHostName());
        connector.setPort(address.getPort());
        server.setConnectors(new Connector[]{connector});
        server.setHandler(servlets);
        server.setStopAtShutdown(true);

        return server;
    }

}
