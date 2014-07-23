package com.kixeye.chassis.bootstrap.app;

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

import com.kixeye.chassis.bootstrap.annotation.App;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Spring app
 *
 * @author dturner@kixeye.com
 */
@App(name = AppIntegrationTest.UNIT_TEST_SPRING_APP, configurationClasses = TestApplicationConfiguration.class, webapp = false)
@Configuration
@ComponentScan(basePackageClasses = TestApplicationConfiguration.class)
public class TestApplicationConfiguration {

    public boolean onInit = false;
    public boolean onDestroy = false;

    @PostConstruct
    public void onInit() {
        onInit = true;
    }

    @PreDestroy
    public void onDestroy() {
        onDestroy = true;
    }

}
