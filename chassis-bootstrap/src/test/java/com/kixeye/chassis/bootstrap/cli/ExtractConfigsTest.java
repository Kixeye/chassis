package com.kixeye.chassis.bootstrap.cli;

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

import com.kixeye.chassis.bootstrap.AppMain;
import com.kixeye.chassis.bootstrap.BootstrapConfigKeys;
import com.kixeye.chassis.bootstrap.TestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * tests for the extract configs feature
 *
 * @author dturner@kixeye.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = com.kixeye.chassis.bootstrap.SpringConfiguration.class)
public class ExtractConfigsTest {

    public static final String KEY1 = "testkey1";
    public static final String KEY2 = "testkey2";
    public static final String VALUE1 = "testvalue1";
    public static final String VALUE2 = "testvalue2";
    public static final String VERSION = "1.0.0";

    @Autowired
    private TestingServer testingServer;

    @Autowired
    private CuratorFramework curatorFramework;

    @Before
    public void before() throws Exception {
        TestUtils.resetArchaius();
        System.setProperty(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName(), VERSION);
    }

    @Test
    public void extractToFile() throws Exception {
        System.setProperty(KEY1, VALUE1);
        System.setProperty(KEY2, VALUE2);


        File configFile = File.createTempFile(ExtractConfigsTest.class.getSimpleName() + ".", ".properties");
        configFile.deleteOnExit();

        AppMain.main(new String[]{"-e", "test", "-l", "info", "-c", "-f", configFile.getPath(), "-a", TestExtractConfigsApp.class.getName(), "-s"});

        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(configFile)) {
            properties.load(is);
        }

        Assert.assertEquals(5, properties.size());
        Assert.assertEquals("atestvalue", properties.get("atestkey"));
        Assert.assertEquals("btestvalue", properties.get("btestkey"));
        Assert.assertEquals("ctestvalue", properties.get("ctestkey"));
        Assert.assertEquals("dtestvalue", properties.get("dtestkey"));
        Assert.assertEquals("etestvalue", properties.get("etestkey"));
        Assert.assertNull(properties.get(KEY1));
        Assert.assertNull(properties.get(KEY2));
    }

    @Test
    public void extractToZookeeper_noBasePathExists() throws Exception {
        String environment = RandomStringUtils.random(10, "abcdefghijk");
        String appName = TestExtractConfigsApp.APP_NAME;

        String base = String.format("/%s/%s/%s/config", environment, appName, VERSION);

        Assert.assertNull(curatorFramework.checkExists().forPath(base));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/atestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/btestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/ctestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/dtestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/etestkey"));

        AppMain.main(new String[]{"-e", environment, "-l", "info", "-c", "-z", testingServer.getConnectString(), "-a", TestExtractConfigsApp.class.getName(), "-s"});

        Assert.assertNotNull(curatorFramework.checkExists().forPath(base));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/atestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/btestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/ctestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/dtestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/etestkey"));
    }

    @Test
    public void extractToZookeeper_basePathExistsNoOverwrite() throws Exception {
        String environment = RandomStringUtils.random(10, "abcdefghijk");
        String appName = TestExtractConfigsApp.APP_NAME;

        String base = String.format("/%s/%s/%s/config", environment, appName, VERSION);

        Assert.assertNull(curatorFramework.checkExists().forPath(base));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/atestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/btestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/ctestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/dtestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/etestkey"));

        AppMain.main(new String[]{"-e", environment, "-l", "info", "-c", "-z", testingServer.getConnectString(), "-a", TestExtractConfigsApp.class.getName(), "-s"});

        Assert.assertNotNull(curatorFramework.checkExists().forPath(base));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/atestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/btestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/ctestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/dtestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/etestkey"));

        TestUtils.resetArchaius();

        AppMain.main(new String[]{"-e", environment, "-l", "info", "-c", "-z", testingServer.getConnectString(), "-a", TestExtractConfigsApp.class.getName(), "-s"});
    }

    @Test
    public void extractToZookeeper_basePathExistsOverwrite() throws Exception {
        String environment = RandomStringUtils.random(10, "abcdefghijk");
        String appName = TestExtractConfigsApp.APP_NAME;

        String base = String.format("/%s/%s/%s/config", environment, appName, VERSION);

        Assert.assertNull(curatorFramework.checkExists().forPath(base));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/atestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/btestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/ctestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/dtestkey"));
        Assert.assertNull(curatorFramework.checkExists().forPath(base + "/etestkey"));

        AppMain.main(new String[]{"-e", environment, "-l", "info", "-c", "-z", testingServer.getConnectString(), "-a", TestExtractConfigsApp.class.getName(), "-s"});

        Assert.assertNotNull(curatorFramework.checkExists().forPath(base));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/atestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/btestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/ctestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/dtestkey"));
        Assert.assertNotNull(curatorFramework.checkExists().forPath(base + "/etestkey"));

        TestUtils.resetArchaius();

        AppMain.main(new String[]{"-e", environment, "-l", "info", "-c", "-z", testingServer.getConnectString(), "-a", TestExtractConfigsApp.class.getName(), "-s", "-cf"});
    }
}
