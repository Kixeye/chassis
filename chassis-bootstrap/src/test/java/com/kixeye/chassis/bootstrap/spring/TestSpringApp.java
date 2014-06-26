package com.kixeye.chassis.bootstrap.spring;

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
