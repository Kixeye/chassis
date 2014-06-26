package com.kixeye.chassis.bootstrap;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.SocketUtils;

import com.google.common.io.Closeables;
import com.kixeye.chassis.bootstrap.BootstrapException.ZookeeperExhibitorUsageException;

/**
 * Tests for CuratorFramework.
 *
 * @author dturner@kixeye.com
 */
public class CuratorFrameworkBuilderTest {

    private TestingServer testingServer;

    @After
    public void after() throws IOException {
        if(testingServer != null){
            Closeables.close(testingServer, true);
        }
    }

    @Test
    public void buildWithZookeeperUnstarted() {
        try (CuratorFramework curatorFramework = new CuratorFrameworkBuilder(false).withZookeeper("localhost:2181").build()) {
        	Assert.assertEquals(CuratorFrameworkState.LATENT, curatorFramework.getState());
        }
    }

    @Test(expected = ZookeeperExhibitorUsageException.class)
    public void buildWithExhibitorThenZookeeper() {
        new CuratorFrameworkBuilder(false).withExhibitors(8080, "localhost").withZookeeper("localhost:2181");
        Assert.fail();
    }

    @Test(expected = ZookeeperExhibitorUsageException.class)
    public void buildWithZookeeperThenExhibitor() {
        new CuratorFrameworkBuilder(false).withZookeeper("localhost:2181").withExhibitors(8080, "localhost");
        Assert.fail();
    }

    @Test
    public void zookeeperStarted() throws Exception {
        testingServer = new TestingServer(SocketUtils.findAvailableTcpPort());
        try (CuratorFramework curatorFramework = new CuratorFrameworkBuilder(true).withZookeeper(testingServer.getConnectString()).build()) {
            Assert.assertEquals(CuratorFrameworkState.STARTED, curatorFramework.getState());
            Assert.assertNull(curatorFramework.checkExists().forPath("/test"));
            curatorFramework.create().forPath("/test");
            Assert.assertNotNull(curatorFramework.checkExists().forPath("/test"));
        }
    }

    @Test
    public void buildWithConfiguration(){
        HashMap<String, Object> map = new HashMap<>();
        map.put(BootstrapConfigKeys.ZOOKEEPER_MAX_RETRIES.getPropertyName(), 1);
        MapConfiguration configuration = new MapConfiguration(map);
        try (CuratorFramework curatorFramework = new CuratorFrameworkBuilder(false).withZookeeper("localhost:2181").withConfiguration(configuration).build()) {}
    }
}
