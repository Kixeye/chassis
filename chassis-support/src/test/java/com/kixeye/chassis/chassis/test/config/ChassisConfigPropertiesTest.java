package com.kixeye.chassis.chassis.test.config;

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
