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

import com.kixeye.chassis.bootstrap.AppMain.Arguments;
import com.kixeye.chassis.bootstrap.Application;
import com.kixeye.chassis.bootstrap.SpringConfiguration;
import com.kixeye.chassis.bootstrap.TestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import java.util.AbstractMap.SimpleEntry;

import static com.kixeye.chassis.bootstrap.configuration.BootstrapConfigKeys.APP_VERSION_KEY;

/**
 * Integration tests for application's which use @BasicApp.
 *
 * @author dturner@kixeye.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfiguration.class)
@DirtiesContext
public class WebAppIntegrationTest {
    private static final String VERSION = "1.0.0";
    public static final String KEY = "testkey";
    public static final String KEY_PLACEHOLDER = "${" + KEY + "}";
    public static final String UNIT_TEST_SPRING_APP = "UnitTestSpringWebApp";

    private String environment;

    @Autowired
    private TestingServer zookeeper;

    @Autowired
    private CuratorFramework curator;

    @Autowired
    private RestTemplate restTemplate;

    private Application application;

    @Before
    public void before() throws Exception {
        System.setProperty(APP_VERSION_KEY.getPropertyName(), VERSION);
        System.setProperty("http.enabled", "true");

        TestUtils.resetArchaius();
        environment = RandomStringUtils.randomAlphabetic(10);
    }

    @After
    public void after() throws Exception {
        if (application != null) {
            application.stop();
        }
    }

    /*
    tests that:
    1) a spring web context starts
    2) we can expose and invoke a spring web-mvc endpoint
    3) the endpoint can access configuration exposed by zookeeper
     */
    @SuppressWarnings("unchecked")
    @Test(timeout = 20000)
    public void testWebServiceWithZookeeperConfig() throws Exception {
        String value = RandomStringUtils.random(10, new char[]{'a', 'b', 'c'});

        TestUtils.addAppProperties(UNIT_TEST_SPRING_APP, environment, VERSION, curator, new SimpleEntry<>(KEY, value));

        Arguments arguments = new Arguments();
        arguments.environment = environment;
        arguments.appClass = TestSpringWebApp.class.getName();
        arguments.zookeeper = zookeeper.getConnectString();
        arguments.skipModuleScanning = true;

        application = new Application(arguments);
        application.start();

        Server httpServer = (Server) application.getApplicationContext().getBean("httpServer");

        int port = ((ServerConnector) httpServer.getConnectors()[0]).getPort();

        String response = restTemplate.getForEntity("http://localhost:" + port + "/getZookeeperProperty", String.class).getBody();

        application.stop();

        Assert.assertEquals(value, response);
        Assert.assertEquals("init", TestSpringWebApp.eventQueue.poll());
        Assert.assertEquals("postConstruct", TestSpringWebApp.eventQueue.poll());
        Assert.assertEquals("preDestroy", TestSpringWebApp.eventQueue.poll());
        Assert.assertEquals("destroy", TestSpringWebApp.eventQueue.poll());
    }

}
