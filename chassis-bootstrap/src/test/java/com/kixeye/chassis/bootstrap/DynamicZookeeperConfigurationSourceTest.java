package com.kixeye.chassis.bootstrap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.SocketUtils;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.DynamicWatchedConfiguration;


/**
 * Tests for DynamicZookeeperConfigurationSource
 *
 * @author dturner@kixeye.com
 */
public class DynamicZookeeperConfigurationSourceTest {
    public static final String DEF_KEY1 = "k1";
    public static final String DEF_VAL1 = "v1";
    public static final String DEF_KEY2 = "k2";
    public static final String DEF_VAL2 = "v2";
    private static final String CONFIG_BASE_PATH = "/config";
    private static TestingServer zookeeper;
    private static CuratorFramework curatorFramework;
    private static AbstractConfiguration defaultConfiguration;
    private String node;
    private ConcurrentCompositeConfiguration config;

    @Before
    public void beforeClass() throws Exception {
        zookeeper = new TestingServer(SocketUtils.findAvailableTcpPort());
        curatorFramework = CuratorFrameworkFactory.newClient(zookeeper.getConnectString(), new RetryOneTime(1000));
        curatorFramework.start();

        curatorFramework.create().forPath(CONFIG_BASE_PATH);

        Map<String, Object> defaults = new HashMap<>();
        defaults.put(DEF_KEY1, DEF_VAL1);
        defaults.put(DEF_KEY2, DEF_VAL2);

        defaultConfiguration = new MapConfiguration(defaults);

        node = UUID.randomUUID().toString();
        config = new ConcurrentCompositeConfiguration();
    }

    @After
    public void afterClass() throws IOException {
    	try {
    		curatorFramework.close();
    	} finally {
	        zookeeper.close();
    	}
    }

    @Test
    public void instanceConfigDoesNotExistAtStartup() {
        config.addConfiguration(new DynamicWatchedConfiguration(new DynamicZookeeperConfigurationSource(curatorFramework, CONFIG_BASE_PATH, node)));
        config.addConfiguration(defaultConfiguration);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));
        Assert.assertEquals(DEF_VAL2, config.getString(DEF_KEY2));
    }

    @Test
    public void instanceConfigExistsAtStartup() throws Exception {
        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node);
        String key = CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1;

        curatorFramework.create().forPath(key, "val1-override".getBytes());

        Assert.assertNotNull(curatorFramework.checkExists().forPath(key));

        config.addConfiguration(new DynamicWatchedConfiguration(new DynamicZookeeperConfigurationSource(curatorFramework, CONFIG_BASE_PATH, node)));
        config.addConfiguration(defaultConfiguration);

        Assert.assertEquals("val1-override", config.getString(DEF_KEY1));
        Assert.assertEquals(DEF_VAL2, config.getString(DEF_KEY2));

    }

    @Test
    public void instanceConfigAddedAfterStartup() throws Exception {
        String instanceConfigNode = CONFIG_BASE_PATH + "/" + node;
        Assert.assertNull(curatorFramework.checkExists().forPath(instanceConfigNode));

        config.addConfiguration(new DynamicWatchedConfiguration(new DynamicZookeeperConfigurationSource(curatorFramework, CONFIG_BASE_PATH, node)));
        config.addConfiguration(defaultConfiguration);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));
        Assert.assertEquals(DEF_VAL2, config.getString(DEF_KEY2));

        //create the instance node
        curatorFramework.create().forPath(instanceConfigNode);

        Thread.sleep(1000);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));
        Assert.assertEquals(DEF_VAL2, config.getString(DEF_KEY2));

        //create a property
        String key = instanceConfigNode + "/" + DEF_KEY1;
        String val = DEF_VAL1 + "--override";

        curatorFramework.create().forPath(key, val.getBytes());

        Thread.sleep(1000);

        Assert.assertEquals(val, config.getString(DEF_KEY1));
        Assert.assertEquals(DEF_VAL2, config.getString(DEF_KEY2));
    }

    @Test
    public void instanceConfigPropertyAdded() throws Exception {
        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node);
        config.addConfiguration(new DynamicWatchedConfiguration(new DynamicZookeeperConfigurationSource(curatorFramework, CONFIG_BASE_PATH, node)));
        config.addConfiguration(defaultConfiguration);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));

        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1, "override".getBytes());

        Thread.sleep(1000);

        Assert.assertEquals("override", config.getString(DEF_KEY1));
    }

    @Test
    public void instanceConfigPropertyUpdated() throws Exception {
        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node);
        config.addConfiguration(new DynamicWatchedConfiguration(new DynamicZookeeperConfigurationSource(curatorFramework, CONFIG_BASE_PATH, node)));
        config.addConfiguration(defaultConfiguration);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));

        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1, "override".getBytes());

        Thread.sleep(1000);

        Assert.assertEquals("override", config.getString(DEF_KEY1));

        curatorFramework.setData().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1, "override2".getBytes());

        Thread.sleep(1000);

        Assert.assertEquals("override2", config.getString(DEF_KEY1));

    }

    @Test
    public void instanceConfigPropertyDeleted() throws Exception {
        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node);
        config.addConfiguration(new DynamicWatchedConfiguration(new DynamicZookeeperConfigurationSource(curatorFramework, CONFIG_BASE_PATH, node)));
        config.addConfiguration(defaultConfiguration);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));

        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1, "override".getBytes());

        Thread.sleep(1000);

        Assert.assertEquals("override", config.getString(DEF_KEY1));

        curatorFramework.delete().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1);

        Thread.sleep(1000);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));
    }

    @Test
    public void instanceConfigDeletedAfterStartup() throws Exception {
        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node);
        config.addConfiguration(new DynamicWatchedConfiguration(new DynamicZookeeperConfigurationSource(curatorFramework, CONFIG_BASE_PATH, node)));
        config.addConfiguration(defaultConfiguration);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));

        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1, "override".getBytes());

        Thread.sleep(1000);

        Assert.assertEquals("override", config.getString(DEF_KEY1));

        curatorFramework.delete().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1);
        curatorFramework.delete().forPath(CONFIG_BASE_PATH + "/" + node);

        Thread.sleep(1000);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));
    }

    @Test
    public void instanceConfigDeletedAndReadded() throws Exception {
        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node);
        config.addConfiguration(new DynamicWatchedConfiguration(new DynamicZookeeperConfigurationSource(curatorFramework, CONFIG_BASE_PATH, node)));
        config.addConfiguration(defaultConfiguration);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));

        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1, "override".getBytes());

        Thread.sleep(1000);

        Assert.assertEquals("override", config.getString(DEF_KEY1));

        curatorFramework.delete().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1);
        curatorFramework.delete().forPath(CONFIG_BASE_PATH + "/" + node);

        Thread.sleep(1000);

        Assert.assertEquals(DEF_VAL1, config.getString(DEF_KEY1));

        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node);

        Thread.sleep(1000);

        curatorFramework.create().forPath(CONFIG_BASE_PATH + "/" + node + "/" + DEF_KEY1, "override".getBytes());

        Thread.sleep(1000);

        Assert.assertEquals("override", config.getString(DEF_KEY1));
    }
}
