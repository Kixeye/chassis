package com.kixeye.chassis.bootstrap.cli;

import com.kixeye.chassis.bootstrap.annotation.BasicApp;

/**
 * App for extract configs test
 *
 * @author dturner@kixeye.com
 */
@BasicApp(name=TestExtractConfigsApp.APP_NAME,propertiesResourceLocation = TestExtractConfigsApp.PROPERTIES_RESOURCE_LOCATION)
public class TestExtractConfigsApp {

    public static final String APP_NAME = "TestExtractConfigsApp";
    public static final String PROPERTIES_RESOURCE_LOCATION = "classpath:com/kixeye/chassis/bootstrap/cli/TestExtractConfigsApp-defaults.properties";
}
