package com.kixeye.chassis.chassis.test.config;

/*
 * #%L
 * Chassis Support
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kixeye.chassis.chassis.ChassisConfiguration;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={ChassisConfigTestConfiguration.class,ChassisConfiguration.class})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChassisConfigPropertiesTest {

    @Test
    public void testBasicProperty() {
        DynamicLongProperty dynamicLongProperty = DynamicPropertyFactory.getInstance().getLongProperty("com.kixeye.platform.chassis.testLong", -1);
        assertEquals(42, dynamicLongProperty.get());
    }
}
