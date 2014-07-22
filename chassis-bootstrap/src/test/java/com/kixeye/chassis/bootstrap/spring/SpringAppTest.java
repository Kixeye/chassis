package com.kixeye.chassis.bootstrap.spring;

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

import static com.kixeye.chassis.bootstrap.configuration.BootstrapConfigKeys.APP_VERSION_KEY;

import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import com.kixeye.chassis.bootstrap.AppMain.Arguments;
import com.kixeye.chassis.bootstrap.Application;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kixeye.chassis.bootstrap.AppMain;
import com.kixeye.chassis.bootstrap.configuration.zookeeper.DynamicZookeeperConfigurationSource;
import com.kixeye.chassis.bootstrap.SpringConfiguration;
import com.kixeye.chassis.bootstrap.TestUtils;
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
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringAppTest {
    private static final String VERSION = "1.0.0";
    public static final String KEY = "testkey";
    public static final String VALUE = "testvalue";
    public static final String UNIT_TEST_SPRING_APP = "UnitTestSpringApp";

    private String environment;

    @Autowired
    private TestingServer zookeeper;

    @Autowired
    private CuratorFramework curator;

    private Application application;

    @Before
    public void before() throws Exception {
        System.setProperty(APP_VERSION_KEY.getPropertyName(), VERSION);
        
        TestUtils.resetArchaius();
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

        if(application != null){
            application.stop();
        }
    }

    /*
    tests that:
    1) we can start the spring context
    2) fetch a spring managed, component scanned bean
    3) the spring bean contains a property injected from zookeeper source.
     */
    @SuppressWarnings("unchecked")
	@Test(timeout = 40000)
    public void runSpringWithZookeeperConfig() throws Exception {
        String value = RandomStringUtils.randomAlphanumeric(5);
        TestUtils.addAppProperties(UNIT_TEST_SPRING_APP, environment, VERSION, curator, new SimpleEntry<String, String>(KEY, value));

        Arguments arguments = new Arguments();
        arguments.environment = environment;
        arguments.appClass = TestSpringApp.class.getName();
        arguments.zookeeper = zookeeper.getConnectString();
        arguments.skipModuleScanning = true;

        application = new Application(arguments).start();

        //grab the TestComponent that has been component-scanned into the app's spring context.
        Map<String,TestComponent> beans = application.getChildApplicationContext().getBeansOfType(TestComponent.class);
        TestComponent testComponent = beans.values().iterator().next();

        //assert that the TestComponent has been injected with a property value from zookeeper.
        Assert.assertEquals(value, testComponent.getTestProperty());
    }

}
