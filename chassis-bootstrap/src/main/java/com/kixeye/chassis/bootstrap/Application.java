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
