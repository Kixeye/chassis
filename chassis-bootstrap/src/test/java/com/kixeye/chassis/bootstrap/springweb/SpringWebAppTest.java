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

import static com.kixeye.chassis.bootstrap.BootstrapConfigKeys.APP_VERSION_KEY;

import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.configuration.AbstractConfiguration;
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

import com.kixeye.chassis.bootstrap.AppMain;
import com.kixeye.chassis.bootstrap.DynamicZookeeperConfigurationSource;
import com.kixeye.chassis.bootstrap.SpringConfiguration;
import com.kixeye.chassis.bootstrap.TestUtils;
import com.kixeye.chassis.bootstrap.spring.SpringWebApplicationContainer;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicWatchedConfiguration;
import com.netflix.config.WatchedConfigurationSource;
import com.netflix.config.source.ZooKeeperConfigurationSource;

/**
 * Integration tests for application's which use @BasicApp.
 *
 * @author dturner@kixeye.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfiguration.class)
@DirtiesContext
public class SpringWebAppTest {
    private static final String VERSION = "1.0.0";
    public static final String KEY = "testkey";
    public static final String KEY_PLACEHOLDER = "${"+KEY + "}";
    public static final String UNIT_TEST_SPRING_APP = "UnitTestSpringWebApp";

    private String environment;
    
    @Autowired
    private TestingServer zookeeper;

    @Autowired
    private CuratorFramework curator;

    @Autowired
    private RestTemplate restTemplate;

    @Before
    public void before() throws Exception {
        AppMain.application = null;
        System.setProperty(APP_VERSION_KEY.getPropertyName(), VERSION);
        System.setProperty("http.enabled", "true");
        
        TestUtils.resetArchaius();
        TestSpringWebApp.reset();
        environment = RandomStringUtils.randomAlphabetic(10);
    }
    
    @After
    public void after() throws Exception {
    	ConcurrentCompositeConfiguration mainConfig = (ConcurrentCompositeConfiguration)ConfigurationManager.getConfigInstance();
    	
    	for (AbstractConfiguration config : mainConfig.getConfigurations()) {
    		if (config instanceof DynamicWatchedConfiguration) {
    			DynamicWatchedConfiguration dyConfig = (DynamicWatchedConfiguration)config;
    			
    			WatchedConfigurationSource configSource = dyConfig.getSource();
    			
    			if (configSource instanceof ZooKeeperConfigurationSource) {
    				ZooKeeperConfigurationSource zkConfigSource = (ZooKeeperConfigurationSource)configSource;
    				zkConfigSource.close();
    				
	    			Field field = ZooKeeperConfigurationSource.class.getDeclaredField("client");
	    			field.setAccessible(true);
	    			
	    			CuratorFramework curator = (CuratorFramework) field.get(zkConfigSource);
	    			curator.close();
    			} else if (configSource instanceof DynamicZookeeperConfigurationSource) {
    				DynamicZookeeperConfigurationSource zkConfigSource = (DynamicZookeeperConfigurationSource)configSource;
    				zkConfigSource.close();
    				
    				Field field = DynamicZookeeperConfigurationSource.class.getDeclaredField("curatorFramework");
	    			field.setAccessible(true);
	    			
	    			CuratorFramework curator = (CuratorFramework) field.get(zkConfigSource);
	    			curator.close();
    			}
    		}
    	}
    	
    	SpringWebApplicationContainer appContainer = (SpringWebApplicationContainer) AppMain.application.getApplicationContainer();
    	appContainer.getApplicationContext().close();
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
        String value = RandomStringUtils.random(10,new char[]{'a','b','c'});
        final String[] args = new String[]{"-e", environment, "-a", TestSpringWebApp.class.getName(), "-s", "-z", zookeeper.getConnectString()};

        TestUtils.addAppProperties(UNIT_TEST_SPRING_APP, environment, VERSION, curator, new SimpleEntry<>(KEY, value));

        //run the spring-based server in a seperate thread so it doesn't block the unit test's thread
        TestUtils.runAppAsServer(args);

        //need to wait till spring is fully started.
        TestUtils.blockUntilAppStarts();

        Server httpServer = (Server) TestUtils.getSpringContextFromApp().getBean("httpServer");
        int port = ((ServerConnector) httpServer.getConnectors()[0]).getPort();

        String response = restTemplate.getForEntity("http://localhost:" + port + "/getZookeeperProperty", String.class).getBody();

        Assert.assertEquals(value, response);
        Assert.assertTrue(TestSpringWebApp.onInit);
    }

}
