package com.kixeye.chassis.bootstrap;

/**
 * Provides hooks to run an application container within an Application managed
 * by java-bootstrap
 *
 * @author dturner@kixeye.com
 */
public interface ApplicationContainer {

    /**
     * Called when the Application is being started. Implementing containers should
     * start at this time.
     */
    void onStart();

    /**
     * Called when the Application is being stopped. Implementing containers should
     * stop at this time.
     */
    void onStop();
}
