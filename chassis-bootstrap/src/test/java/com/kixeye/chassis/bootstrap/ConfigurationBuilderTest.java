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
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.kixeye.chassis.bootstrap.BootstrapException.ApplicationConfigurationNotFoundException;
import com.kixeye.chassis.bootstrap.aws.ServerInstanceContext;
import com.kixeye.chassis.bootstrap.configuration.BootstrapConfigKeys;
import com.kixeye.chassis.bootstrap.configuration.ConfigurationBuilder;
import com.kixeye.chassis.bootstrap.configuration.zookeeper.ZookeeperConfigurationProvider;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.PropertySource;

import com.amazonaws.regions.Regions;
import com.kixeye.chassis.bootstrap.BootstrapException.ConflictingModuleConfigurationKeysException;
import com.kixeye.chassis.bootstrap.BootstrapException.ResourceLoadingException;
import com.netflix.config.DynamicPropertyFactory;

/**
 * Unit tests for ConfigurationBuilder
 *
 * @author dturner@kixeye.com
 */
public class ConfigurationBuilderTest {
    public static final String TEST_MODULE_CONFIG_PROPERTIES = "${java.io.tmpdir}/unittest/testmodule.properties";
    public static final String TEST_MODULE_CONFIG_PROPERTIES_SPRING_PATH = "file://" + TEST_MODULE_CONFIG_PROPERTIES;

    public static final String TEST_MODULE_CONFIG_PROPERTIES_2 = "${java.io.tmpdir}/unittest/testmodule2.properties";
    public static final String TEST_MODULE_CONFIG_PROPERTIES_SPRING_PATH_2 = "file://" + TEST_MODULE_CONFIG_PROPERTIES_2;


    public static final String TEST_APP_CONFIG_PROPERTIES = "${java.io.tmpdir}/unittest/testapp.properties";
    public static final String TEST_APP_CONFIG_PROPERTIES_SPRING_PATH = "file://" + TEST_APP_CONFIG_PROPERTIES;
    public static final String ENVIRONMENT = "unittest";
    public static final String APP_NAME = "testapp";
    public static final String APP_VERSION = "1.0.0";
    public static final String ZOOKEEPER_CONFIG_ROOT = "/" + ENVIRONMENT + "/" + APP_NAME + "/" + APP_VERSION + "/config";
    public static final String MODULE_1_KEY_1 = "m1k1";
    public static final String MODULE_1_VALUE_1 = "m1v1";
    public static final String MODULE_1_KEY_2 = "m1k2";
    public static final String MODULE_1_VALUE_2 = "m1v1";
    public static final String MODULE_1_KEY_3 = "m1k3";
    public static final String MODULE_1_VALUE_3 = "m1v3";
    private static final Object MODULE_2_KEY_1 = "m2k1";
    private static final Object MODULE_2_VALUE_1 = "m2v2";
    private static TestingServer zookeeperServer;
    private static CuratorFramework curatorFramework;
    private static ConfigurationBuilder configurationBuilder;
    private Set<Path> filesCreated = new HashSet<>();

    private static void initializeZookeeper() throws Exception {
        zookeeperServer = new TestingServer();
        curatorFramework = CuratorFrameworkFactory.newClient(zookeeperServer.getConnectString(), new RetryOneTime(1000));
        curatorFramework.start();
    }

    private static void teardownZookeeper() throws IOException {
        curatorFramework.close();
        zookeeperServer.stop();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
    public void setup() throws Exception {
    	initializeZookeeper();
    	
        configurationBuilder = new ConfigurationBuilder(APP_NAME, ENVIRONMENT, true, BootstrapConfiguration.REFLECTIONS);
        configurationBuilder.withAppVersion(APP_VERSION);

        TestUtils.resetArchaius();
        resetZookeeper();

        TestUtils.writePropertiesToFile(TEST_MODULE_CONFIG_PROPERTIES, filesCreated, new SimpleEntry[]{new SimpleEntry(MODULE_1_KEY_1, MODULE_1_VALUE_1), new SimpleEntry(MODULE_1_KEY_2, MODULE_1_VALUE_2), new SimpleEntry(MODULE_1_KEY_3, MODULE_1_VALUE_3)});
        TestUtils.writePropertiesToFile(TEST_MODULE_CONFIG_PROPERTIES_2, filesCreated, new SimpleEntry(MODULE_2_KEY_1, MODULE_2_VALUE_1));
    }

    @After
    public void teardown() throws Exception {
    	configurationBuilder.close();
    	
        TestUtils.delete(filesCreated);

        teardownZookeeper();

        TestUtils.resetArchaius();
    }

    private void resetZookeeper() throws Exception {
        curatorFramework.create().creatingParentsIfNeeded().forPath(ZOOKEEPER_CONFIG_ROOT);
    }

    @Test
    public void defaultModuleConfigurations() {
        Configuration configuration = configurationBuilder.build();

        Assert.assertEquals(MODULE_1_VALUE_1, configuration.getString(MODULE_1_KEY_1));
        Assert.assertEquals(MODULE_1_VALUE_2, configuration.getString(MODULE_1_KEY_2));
        Assert.assertEquals(MODULE_1_VALUE_3, configuration.getString(MODULE_1_KEY_3));
    }

    @Test
    public void buildConfigurationInAws(){
        String az = "testaz";
        String instanceId = RandomStringUtils.randomAlphabetic(10);
        String region = Regions.DEFAULT_REGION.getName();

        ServerInstanceContext serverInstanceContext = EasyMock.createMock(ServerInstanceContext.class);
        EasyMock.expect(serverInstanceContext.getAvailabilityZone()).andReturn(az);
        EasyMock.expect(serverInstanceContext.getInstanceId()).andReturn(instanceId);
        EasyMock.expect(serverInstanceContext.getRegion()).andReturn(region);
        EasyMock.expect(serverInstanceContext.getPrivateIp()).andReturn("127.0.0.1");
        EasyMock.expect(serverInstanceContext.getPublicIp()).andReturn(null);

        EasyMock.replay(serverInstanceContext);

        Configuration configuration = configurationBuilder.withServerInstanceContext(serverInstanceContext).build();

        Assert.assertEquals(az, configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_AVAILABILITY_ZONE.getPropertyName()));
        Assert.assertEquals(instanceId, configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_ID.getPropertyName()));
        Assert.assertEquals(region, configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_REGION.getPropertyName()));
        Assert.assertEquals("127.0.0.1", configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_PRIVATE_IP.getPropertyName()));
        Assert.assertEquals(null, configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_PUBLIC_IP.getPropertyName()));

        EasyMock.verify(serverInstanceContext);
    }

    @Test
    public void buildConfigurationOutsideAws(){
        Configuration configuration = configurationBuilder.build();

        Assert.assertEquals(ConfigurationBuilder.LOCAL_INSTANCE_ID, configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_ID.getPropertyName()));
        Assert.assertEquals(ConfigurationBuilder.UNKNOWN, configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_AVAILABILITY_ZONE.getPropertyName()));
        Assert.assertEquals(ConfigurationBuilder.UNKNOWN, configuration.getString(BootstrapConfigKeys.AWS_INSTANCE_REGION.getPropertyName()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    public void applicationConfigurations() {
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES, filesCreated, new SimpleEntry(MODULE_1_KEY_3, MODULE_1_VALUE_3 + "-override"));

        configurationBuilder.withApplicationProperties("file://" + TEST_APP_CONFIG_PROPERTIES);
        Configuration configuration = configurationBuilder.build();

        Assert.assertEquals(MODULE_1_VALUE_1, configuration.getString(MODULE_1_KEY_1));
        Assert.assertEquals(MODULE_1_VALUE_2, configuration.getString(MODULE_1_KEY_2));
        Assert.assertEquals(MODULE_1_VALUE_3 + "-override", configuration.getString(MODULE_1_KEY_3));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(expected = ResourceLoadingException.class)
    public void invalidApplicationConfigurationPath() {
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES, filesCreated, new SimpleEntry(MODULE_1_KEY_3, MODULE_1_VALUE_3 + "-override"));

        configurationBuilder.withApplicationProperties("file://" + TEST_APP_CONFIG_PROPERTIES + ".foo");
        Configuration configuration = configurationBuilder.build();

        Assert.assertEquals(MODULE_1_VALUE_1, configuration.getString(MODULE_1_KEY_1));
        Assert.assertEquals(MODULE_1_VALUE_2, configuration.getString(MODULE_1_KEY_2));
        Assert.assertEquals(MODULE_1_VALUE_3 + "-override", configuration.getString(MODULE_1_KEY_3));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void applicationConfigurationsWithEnvironmentConfiguration() {
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES, filesCreated, new SimpleEntry(MODULE_1_KEY_2, MODULE_1_VALUE_2 + "-override"), new SimpleEntry(MODULE_1_KEY_3, MODULE_1_VALUE_3 + "-override"));
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES.replace(".properties", "." + ENVIRONMENT + ".properties"), filesCreated, new SimpleEntry(MODULE_1_KEY_2, MODULE_1_VALUE_2 + "-override"));

        configurationBuilder.withApplicationProperties("file://" + TEST_APP_CONFIG_PROPERTIES);
        Configuration configuration = configurationBuilder.build();

        Assert.assertEquals(MODULE_1_VALUE_1, configuration.getString(MODULE_1_KEY_1));
        Assert.assertEquals(MODULE_1_VALUE_2 + "-override", configuration.getString(MODULE_1_KEY_2));
        Assert.assertEquals(MODULE_1_VALUE_3 + "-override", configuration.getString(MODULE_1_KEY_3));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void fetchFromArchaius() {
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES, filesCreated, new SimpleEntry(MODULE_1_KEY_2, MODULE_1_VALUE_2 + "-override"), new SimpleEntry(MODULE_1_KEY_3, MODULE_1_VALUE_3 + "-override"));
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES.replace(".properties", "." + ENVIRONMENT + ".properties"), filesCreated, new SimpleEntry(MODULE_1_KEY_2, MODULE_1_VALUE_2 + "-override"));

        configurationBuilder.withApplicationProperties(TEST_APP_CONFIG_PROPERTIES_SPRING_PATH);
        Configuration configuration = configurationBuilder.build();

        Assert.assertEquals(MODULE_1_VALUE_1, configuration.getString(MODULE_1_KEY_1));
        Assert.assertEquals(MODULE_1_VALUE_2 + "-override", configuration.getString(MODULE_1_KEY_2));
        Assert.assertEquals(MODULE_1_VALUE_3 + "-override", configuration.getString(MODULE_1_KEY_3));

        Assert.assertEquals(MODULE_1_VALUE_1, DynamicPropertyFactory.getInstance().getStringProperty(MODULE_1_KEY_1, null).getValue());
        Assert.assertEquals(MODULE_1_VALUE_2 + "-override", DynamicPropertyFactory.getInstance().getStringProperty(MODULE_1_KEY_2, null).getValue());
        Assert.assertEquals(MODULE_1_VALUE_3 + "-override", DynamicPropertyFactory.getInstance().getStringProperty(MODULE_1_KEY_3, null).getValue());

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    public void missingZookeeperConfig_writeDefaults() throws Exception {
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES, filesCreated, new SimpleEntry(BootstrapConfigKeys.PUBLISH_DEFAULTS_KEY.getPropertyName(), "true"));
        TestUtils.deleteAll(curatorFramework);

        Assert.assertNull(curatorFramework.checkExists().forPath(ZOOKEEPER_CONFIG_ROOT));
        configurationBuilder.withConfigurationProvider(new ZookeeperConfigurationProvider(zookeeperServer.getConnectString()));
        configurationBuilder.withApplicationProperties("file://" + TEST_APP_CONFIG_PROPERTIES);
        configurationBuilder.build();

        Assert.assertNotNull(curatorFramework.checkExists().forPath(ZOOKEEPER_CONFIG_ROOT));
        Assert.assertEquals(MODULE_1_VALUE_1, new String(curatorFramework.getData().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_1_KEY_1)));
        Assert.assertEquals(MODULE_1_VALUE_2, new String(curatorFramework.getData().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_1_KEY_2)));
        Assert.assertEquals(MODULE_1_VALUE_3, new String(curatorFramework.getData().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_1_KEY_3)));
        Assert.assertEquals(MODULE_2_VALUE_1, new String(curatorFramework.getData().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_2_KEY_1)));
        Assert.assertEquals(4, curatorFramework.getChildren().forPath(ZOOKEEPER_CONFIG_ROOT).size());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    public void missingZookeeperConfig_writeDefaultsEnvironmentFileOverride() throws Exception {
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES, filesCreated, new SimpleEntry(BootstrapConfigKeys.PUBLISH_DEFAULTS_KEY.getPropertyName(), "false"));
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES.replace(".properties", "." + ENVIRONMENT + ".properties"), filesCreated, new SimpleEntry(BootstrapConfigKeys.PUBLISH_DEFAULTS_KEY.getPropertyName(), "true"));
        TestUtils.deleteAll(curatorFramework);

        Assert.assertNull(curatorFramework.checkExists().forPath(ZOOKEEPER_CONFIG_ROOT));

        configurationBuilder.withConfigurationProvider(new ZookeeperConfigurationProvider(zookeeperServer.getConnectString()));
        configurationBuilder.withApplicationProperties("file://" + TEST_APP_CONFIG_PROPERTIES);
        configurationBuilder.build();

        Assert.assertNotNull(curatorFramework.checkExists().forPath(ZOOKEEPER_CONFIG_ROOT));
        Assert.assertEquals(MODULE_1_VALUE_1, new String(curatorFramework.getData().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_1_KEY_1)));
        Assert.assertEquals(MODULE_1_VALUE_2, new String(curatorFramework.getData().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_1_KEY_2)));
        Assert.assertEquals(MODULE_1_VALUE_3, new String(curatorFramework.getData().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_1_KEY_3)));
        Assert.assertEquals(MODULE_2_VALUE_1, new String(curatorFramework.getData().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_2_KEY_1)));
        Assert.assertEquals(4, curatorFramework.getChildren().forPath(ZOOKEEPER_CONFIG_ROOT).size());
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Test(expected = ApplicationConfigurationNotFoundException.class)
    public void missingZookeeperConfig_writeDefaultsDisabledInConfig() throws Exception {
        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES, filesCreated, new SimpleEntry(BootstrapConfigKeys.PUBLISH_DEFAULTS_KEY.getPropertyName(), "false"));
        TestUtils.deleteAll(curatorFramework);

        Assert.assertNull(curatorFramework.checkExists().forPath(ZOOKEEPER_CONFIG_ROOT));
        configurationBuilder.withConfigurationProvider(new ZookeeperConfigurationProvider(zookeeperServer.getConnectString()));
        configurationBuilder.withApplicationProperties("file://" + TEST_APP_CONFIG_PROPERTIES);
        configurationBuilder.build();
    }

    @Test(expected = ApplicationConfigurationNotFoundException.class)
    public void missingZookeeperConfig() throws Exception {
        TestUtils.deleteAll(curatorFramework);

        Assert.assertNull(curatorFramework.checkExists().forPath(ZOOKEEPER_CONFIG_ROOT));
        configurationBuilder.withConfigurationProvider(new ZookeeperConfigurationProvider(zookeeperServer.getConnectString()));
        configurationBuilder.build();
    }

    @Test
    public void zookeeperHappy() throws Exception {
        String key = ZOOKEEPER_CONFIG_ROOT + "/" + MODULE_1_KEY_1;
        curatorFramework.create().creatingParentsIfNeeded().forPath(key, MODULE_1_VALUE_1.getBytes());
        Assert.assertEquals(MODULE_1_VALUE_1, new String(curatorFramework.getData().forPath(key)));

        configurationBuilder.withConfigurationProvider(new ZookeeperConfigurationProvider(zookeeperServer.getConnectString()));
        Configuration configuration = configurationBuilder.build();

        Assert.assertEquals(MODULE_1_VALUE_1, configuration.getString(MODULE_1_KEY_1));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void zookeeperOverridesApplicationProperties() throws Exception {
        String key1 = UUID.randomUUID().toString();
        String value1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        String value2 = UUID.randomUUID().toString();


        TestUtils.writePropertiesToFile(TEST_APP_CONFIG_PROPERTIES, filesCreated, new SimpleEntry(key1, value1), new SimpleEntry(key2, value2));

        curatorFramework.create().creatingParentsIfNeeded().forPath(ZOOKEEPER_CONFIG_ROOT + "/" + key1, "zookeepervalue".getBytes());

        configurationBuilder.withConfigurationProvider(new ZookeeperConfigurationProvider(zookeeperServer.getConnectString()));
        configurationBuilder.withApplicationProperties(TEST_APP_CONFIG_PROPERTIES_SPRING_PATH);
        Configuration configuration = configurationBuilder.build();

        //assert that the zookeeper config overrode the app properties config
        Assert.assertEquals("zookeepervalue", configuration.getString(key1));
        //assert that the proper which exists in app properties but not in zookeeper is found.
        Assert.assertEquals(value2, configuration.getString(key2));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(expected = ConflictingModuleConfigurationKeysException.class)
    public void moduleDefaultsWithConflictingKeys(){
        //add a key/value pair that will conflict with properties in TEST_MODULE_CONFIG_PROPERTIES
        TestUtils.writePropertiesToFile(TEST_MODULE_CONFIG_PROPERTIES_2, filesCreated, new SimpleEntry(MODULE_1_KEY_1, MODULE_1_VALUE_1));
        configurationBuilder.build();
    }

    @PropertySource(TEST_MODULE_CONFIG_PROPERTIES_SPRING_PATH)
    private static final class TestModuleConfiguration {
    }

    @PropertySource(TEST_MODULE_CONFIG_PROPERTIES_SPRING_PATH_2)
    private static final class TestModuleConfiguration2 {
    }

}
