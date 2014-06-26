package com.kixeye.chassis.bootstrap;

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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for ZookeeperConfigurationWriter
 *
 * @author dturner@kixeye.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfiguration.class)
@DirtiesContext
public class ZookeeperConfigurationWriterTest {

    @Autowired
    private TestingServer testingServer;

    @Autowired
    private CuratorFramework curatorFramework;

    private String applicationName;
    private String environmentName;
    private String version;
    private String base;
    private String key1;
    private String val1;
    private String key2;
    private String val2;
    Map<String, Object> props = new HashMap<>();

    @Before
    public void before() {
        applicationName = RandomUtils.nextInt() + "";
        environmentName = RandomUtils.nextInt() + "";
        version = RandomUtils.nextInt() + "";

        base = "/" + environmentName + "/" + applicationName + "/" + version + "/config";

        props.clear();

        key1 = RandomStringUtils.random(10,"abcdefghijkl");
        val1 = RandomStringUtils.random(10,"abcdefghijkl");
        key2 = RandomStringUtils.random(10,"abcdefghijkl");
        val2 = RandomStringUtils.random(10,"abcdefghijkl");
        props.put(key1, val1);
        props.put(key2, val2);
    }


    @Test
    public void noBasePathExists() throws Exception {
        Assert.assertNull(curatorFramework.checkExists().forPath(base));

        try (CuratorFramework zkCurator = createCuratorFramework()) {
	        ZookeeperConfigurationWriter writer = new ZookeeperConfigurationWriter(applicationName, environmentName, version, zkCurator, false);
	        writer.write(new MapConfiguration(props), new DefaultPropertyFilter());
	
	        Assert.assertEquals(val1, new String(curatorFramework.getData().forPath(base + "/" + key1)));
        }
    }

    @Test
    public void partialBasePathExists() throws Exception {
        Assert.assertNull(curatorFramework.checkExists().forPath(base));

        curatorFramework.create().forPath("/" + environmentName);

        Assert.assertNotNull(curatorFramework.checkExists().forPath("/" + environmentName));

        try (CuratorFramework zkCurator = createCuratorFramework()) {
	        ZookeeperConfigurationWriter writer = new ZookeeperConfigurationWriter(applicationName, environmentName, version, zkCurator, false);
			writer.write(new MapConfiguration(props), new DefaultPropertyFilter());
	
	        Assert.assertEquals(val1, new String(curatorFramework.getData().forPath(base + "/" + key1)));
        }
    }

    @Test
    public void basePathExists_noOverwrite() throws Exception {
        Assert.assertNull(curatorFramework.checkExists().forPath(base));

        try (CuratorFramework zkCurator = createCuratorFramework()) {
	        ZookeeperConfigurationWriter writer = new ZookeeperConfigurationWriter(applicationName, environmentName, version, zkCurator, false);
			writer.write(new MapConfiguration(props), new DefaultPropertyFilter());
		
	        Assert.assertEquals(val1, new String(curatorFramework.getData().forPath(base + "/" + key1)));
	        Assert.assertEquals(val2, new String(curatorFramework.getData().forPath(base + "/" + key2)));
	
	        try {
	            writer.write(new MapConfiguration(props), new DefaultPropertyFilter());
	            Assert.fail();
	        } catch (BootstrapException e) {
	            //expected
	        }
        }
    }

    @Test
    public void basePathExists_overwrite() throws Exception {
        Assert.assertNull(curatorFramework.checkExists().forPath(base));

        try (CuratorFramework zkCurator = createCuratorFramework()) {
	        ZookeeperConfigurationWriter writer = new ZookeeperConfigurationWriter(applicationName, environmentName, version, zkCurator, true);
	
	        writer.write(new MapConfiguration(props), new DefaultPropertyFilter());
	
	        Assert.assertEquals(val1, new String(curatorFramework.getData().forPath(base + "/" + key1)));
	        Assert.assertEquals(val2, new String(curatorFramework.getData().forPath(base + "/" + key2)));
	
	        String key3 = RandomUtils.nextInt() + "";
	        String val3 = RandomUtils.nextInt() + "";
	        String newVal1 = RandomUtils.nextInt() + "";
	
	        props.clear();
	
	        //updates key1
	        props.put(key1, newVal1);
	        //adds key3
	        props.put(key3, val3);
	
	        //key2 should be deleted
	
	        writer.write(new MapConfiguration(props), new DefaultPropertyFilter());
	
	        Assert.assertEquals(newVal1, new String(curatorFramework.getData().forPath(base + "/" + key1)));
	        Assert.assertEquals(val3, new String(curatorFramework.getData().forPath(base + "/" + key3)));
	        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/" + key2));
        }
    }


    private CuratorFramework createCuratorFramework() {
        CuratorFramework curator = CuratorFrameworkFactory.newClient(
                testingServer.getConnectString(),
                5000,
                5000,
                new RetryOneTime(1000));
        curator.start();
        return curator;
    }
}
