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

import java.io.IOException;
import java.util.HashMap;

import com.kixeye.chassis.bootstrap.configuration.BootstrapConfigKeys;
import com.kixeye.chassis.bootstrap.configuration.zookeeper.CuratorFrameworkBuilder;
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
