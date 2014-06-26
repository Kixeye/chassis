package com.kixeye.chassis.bootstrap.springweb;

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

import com.kixeye.chassis.bootstrap.annotation.Init;
import com.kixeye.chassis.bootstrap.annotation.SpringApp;
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
import java.net.InetSocketAddress;

/**
 * Spring web app configuration
 *
 * @author dturner@kixeye.com
 */
@SpringApp(name = SpringWebAppTest.UNIT_TEST_SPRING_APP, configurationClasses = TestSpringWebApp.class, webapp = true)
@Configuration
@ComponentScan(basePackageClasses = TestSpringWebApp.class)
public class TestSpringWebApp extends DelegatingWebMvcConfiguration {

    public static boolean onInit = false;

    @Init
    public static void onInit(org.apache.commons.configuration.Configuration configuration) {
        onInit = true;
    }

    public static void reset() {
        onInit = false;
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
