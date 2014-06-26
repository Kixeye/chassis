package com.kixeye.chassis.bootstrap.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A Spring component used to test component scanning and property
 * injection.
 *
 * @author dturner@kixeye.com
 */
@Component
public class TestComponent {
    @Value("${testkey}")
    private String testProperty;

    public String getTestProperty() {
        return testProperty;
    }
}
