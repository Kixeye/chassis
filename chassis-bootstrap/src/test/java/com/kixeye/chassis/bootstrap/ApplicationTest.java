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

import com.kixeye.chassis.bootstrap.AppMain.Arguments;
import com.kixeye.chassis.bootstrap.annotation.App;
import com.kixeye.chassis.bootstrap.annotation.Destroy;
import com.kixeye.chassis.bootstrap.annotation.Init;
import com.kixeye.chassis.bootstrap.configuration.BootstrapConfigKeys;
import com.kixeye.chassis.bootstrap.configuration.ConfigurationProvider;
import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Application}
 *
 * @author dturner@kixeye.com
 */
public class ApplicationTest {

    private Application application;

    @Before
    public void before() throws Exception{
        TestUtils.resetArchaius();
    }

    @After
    public void after(){
        if(application == null){
            return;
        }
        application.stop();
    }

    @Test
    public void testAppWithNoConfigurationClasses() {
        Arguments arguments = new Arguments();
        arguments.appClass = TestApp.class.getName();
        arguments.skipModuleScanning = true;
        arguments.environment = "test";
        arguments.skipServerInstanceContextInitialization = true;

        System.setProperty(BootstrapConfigKeys.APP_VERSION_KEY.getPropertyName(), "1.0");

         application = new Application(arguments);

        Assert.assertEquals(false, TestApp.initCalled);
        Assert.assertEquals(false, TestApp.destroyCalled);

        application.start();

        Assert.assertEquals(true, TestApp.initCalled);

        application.stop();

        Assert.assertEquals(true, TestApp.destroyCalled);
    }

    @App(name = "TestApp")
    private static class TestApp {

        private static boolean initCalled = false;
        private static boolean destroyCalled = false;

        @Init
        public static void init(Configuration configuration, ConfigurationProvider configurationProvider) {
            initCalled = true;
        }

        @Destroy
        public static void destroy(){
            destroyCalled = true;
        }
    }
}
