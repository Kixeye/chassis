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

import com.kixeye.chassis.bootstrap.annotation.Destroy;
import com.kixeye.chassis.bootstrap.annotation.Init;
import com.kixeye.chassis.bootstrap.annotation.SpringApp;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring app
 *
 * @author dturner@kixeye.com
 */
@SpringApp(name = SpringAppTest.UNIT_TEST_SPRING_APP, configurationClasses = TestSpringApp.class, webapp = false)
@Configuration
@ComponentScan(basePackageClasses = TestSpringApp.class)
public class TestSpringApp {

    public static boolean onInit = false;
    public static boolean onDestroy = false;

    public static org.apache.commons.configuration.Configuration configuration;

    @Init
    public static void onInit(org.apache.commons.configuration.Configuration configuration) {
        onInit = true;
    }

    @Destroy
    public static void onDestroy() {
        onDestroy = true;
    }

    public static void reset() {
        onInit = false;
        onDestroy = false;
        configuration = null;
    }
}
