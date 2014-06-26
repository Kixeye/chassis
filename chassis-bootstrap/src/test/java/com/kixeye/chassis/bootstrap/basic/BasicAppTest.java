package com.kixeye.chassis.bootstrap.basic;

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
import com.kixeye.chassis.bootstrap.SpringConfiguration;
import com.kixeye.chassis.bootstrap.TestUtils;
import com.kixeye.chassis.bootstrap.annotation.BasicApp;
import com.kixeye.chassis.bootstrap.annotation.Destroy;
import com.kixeye.chassis.bootstrap.annotation.Init;
import com.kixeye.chassis.bootstrap.annotation.OnStart;
import com.kixeye.chassis.bootstrap.annotation.OnStop;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;

import static com.kixeye.chassis.bootstrap.BootstrapConfigKeys.APP_VERSION_KEY;

/**
 * Integration tests for application's which use @BasicApp.
 *
 * @author dturner@kixeye.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfiguration.class)
public class BasicAppTest {
    private static final String VERSION = "1.0.0";
    public static final String KEY = "testkey";
    public static final String VALUE = "testvalue";
    private String environment;

    @Autowired
    private TestingServer zookeeper;

    @Autowired
    private CuratorFramework curator;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void beforeClass() throws IOException {
        AppMain.application = null;
        File propertiesFile = File.createTempFile("unittest-",".properties");
        propertiesFile.deleteOnExit();

        TestUtils.writePropertiesToFile(propertiesFile.getPath(), new HashSet<Path>(), new SimpleEntry<String, String>(KEY, VALUE));

        System.setProperty(APP_VERSION_KEY.getPropertyName(), VERSION);
        System.setProperty("configFile",propertiesFile.getPath());
    }

    @Before
    public void before() throws Exception {
        TestUtils.resetArchaius();
        UnitTestApp.reset();
        environment = RandomStringUtils.randomAlphabetic(10);
    }

    @Test(timeout = 40000)
    public void localConfig() throws Exception {
        String[] args = new String[]{"-e", environment, "-a", UnitTestApp.class.getName(), "-s"};
        Assert.assertFalse(UnitTestApp.onDestroy);
        Assert.assertFalse(UnitTestApp.onInit);

        runApp(args);

        UnitTestApp instance = TestUtils.getCustomAppInstance();

        Assert.assertTrue(instance.started);
        Assert.assertTrue(instance.stopped);
        Assert.assertTrue(UnitTestApp.onDestroy);
        Assert.assertTrue(UnitTestApp.onInit);

        //proves that the Configuration given to the application is correctly set up.
        Assert.assertEquals(VERSION, UnitTestApp.configuration.getString(APP_VERSION_KEY.getPropertyName()));

        //proves that config file loading from @App class is working
        Assert.assertEquals(VALUE, UnitTestApp.configuration.getString(KEY));
    }

    private void runApp(String[] args) throws Exception {
        Thread watcher = TestUtils.stopAppAfterLaunch();
        AppMain.main(args);
        watcher.join();
    }

	@Test(timeout = 40000)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void zookeeperConfig() throws Exception {
        String value = RandomStringUtils.randomAlphanumeric(5);
        String[] args = new String[]{"-e", environment, "-a", UnitTestApp.class.getName(), "-s", "-z", zookeeper.getConnectString()};

        TestUtils.addAppProperties("UnitTestApp", environment, VERSION, curator, new SimpleEntry(KEY,value));

        runApp(args);

        UnitTestApp instance = TestUtils.getCustomAppInstance();

        Assert.assertTrue(instance.started);
        Assert.assertTrue(instance.stopped);
        Assert.assertTrue(UnitTestApp.onDestroy);
        Assert.assertTrue(UnitTestApp.onInit);

        //proves that the Configuration given to the application is correctly set up.
        Assert.assertEquals(VERSION, UnitTestApp.configuration.getString(APP_VERSION_KEY.getPropertyName()));

        //proves that config from zookeeper is working
        Assert.assertEquals(value, UnitTestApp.configuration.getString(KEY));
    }

    @BasicApp(name="UnitTestApp", propertiesResourceLocation = "file:${configFile}")
    public static class UnitTestApp {

        private static boolean onInit = false;
        private static boolean onDestroy = false;
        private boolean started = false;
        private boolean stopped = false;

        private static org.apache.commons.configuration.Configuration configuration;

        @Init
        public static void onInit(org.apache.commons.configuration.Configuration configuration) {
            onInit = true;
            UnitTestApp.configuration = configuration;
        }

        @Destroy
        public static void onDestroy() {
            onDestroy = true;
        }

        @OnStart
        public void onStart() {
            started = true;
        }

        @OnStop
        public void onStop() {
            stopped = true;
        }

        public static void reset() {
            onInit = false;
            onDestroy = false;
            configuration = null;
        }
    }
}
