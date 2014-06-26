package com.kixeye.chassis.bootstrap;

/**
 * Describes an application managed by java-bootstrap.
 *
 * @author dturner@kixeye.com
 */
public interface Application {
    /**
     * Start the application
     */
    void start();

    /**
     * Stop the application
     */
    void stop();

    /**
     * Get the name of the Application
     * @return
     */
    String getName();

    /**
     * return whether or not the application is running
     * @return
     */
    boolean isRunning();

    /**
     * Return the definition
     * @return
     */
    ApplicationDefinition getDefinition();

    /**
     * Return the applicationContainer of the Application
     */
    ApplicationContainer getApplicationContainer();
}
