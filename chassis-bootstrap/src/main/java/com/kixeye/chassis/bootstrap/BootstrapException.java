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

import java.util.Arrays;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.curator.ensemble.exhibitor.Exhibitors;


/**
 * An error occurred attempting to bootstrap an application.
 *
 * @author dturner@kixeye.com
 */
@SuppressWarnings("serial")
public class BootstrapException extends RuntimeException {
    public BootstrapException(String message) {
        super(message);
    }

    public BootstrapException(String message, Exception e) {
        super(message, e);
    }

    public static void resourceLoadingFailed(String path, String originalPath, Exception e) {
        throw new ResourceLoadingException(path, originalPath, e);
    }

    public static void resourceLoadingFailed(String path, Exception e) {
        throw new ResourceLoadingException(path, e);
    }

    public static void missingApplicationVersion() {
        throw new MissingApplicationVersionException();
    }

    public static void zookeeperInitializationFailed(String zookeeperHost, String configPath, Exception e) {
        throw new ZookeeperInitializationException(zookeeperHost, configPath, e);
    }

    public static void zookeeperInitializationFailed(String zookeeperHost, Exhibitors exhibitors, Exception e) {
        throw new ZookeeperInitializationException(zookeeperHost, exhibitors, e);
    }

    public static void configurationNotFound(String message, Exception e) {
        throw new ApplicationConfigurationNotFoundException(message, e);
    }

    public static void moduleKeysConflictFound(String propertyFile, String[] propertyFiles) {
        throw new ConflictingModuleConfigurationKeysException(propertyFile, propertyFiles);
    }

    public static void zookeeperExhibitorConflict() {
        throw new ZookeeperExhibitorUsageException();
    }

    public static class MissingApplicationVersionException extends BootstrapException {

        public MissingApplicationVersionException() {
            super("application version not found.  Version should be defined in application jar's MANIFEST.MF (Implementation-Version), as system property (app.version) or application's configuration (app.version).");
        }
    }

    public static class ZookeeperExhibitorUsageException extends BootstrapException{

        public ZookeeperExhibitorUsageException() {
            super("Exhibitor and Zookeeper cannot be configured together. One or the other must be used.");
        }
    }

    public static class ResourceLoadingException extends BootstrapException {

        public ResourceLoadingException(String path, String originalPath, Exception e) {
            super("Unable to load application configuration resource " + path + ". Original path: " + originalPath, e);
        }

        public ResourceLoadingException(String path, Exception e) {
            super("Unable to load application configuration resource " + path + ".", e);
        }
    }

    public static class ZookeeperInitializationException extends BootstrapException {
        public ZookeeperInitializationException(String zookeeperHost, String configPath, Exception e) {
            super("Unable to initialize zookeeper configuration for zookeeper " + zookeeperHost + " at path " + configPath + ".", e);
        }

        public ZookeeperInitializationException(String zookeeperHost, Exhibitors exhibitors, Exception e) {
            super("Unable to initialize zookeeper configuration for zookeeper " + zookeeperHost + ". Exhibitors:" + ReflectionToStringBuilder.reflectionToString(exhibitors), e);
        }
    }

    public static class ApplicationConfigurationNotFoundException extends BootstrapException {
        public ApplicationConfigurationNotFoundException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class ConflictingModuleConfigurationKeysException extends BootstrapException {
        public ConflictingModuleConfigurationKeysException(String propertyFile, String[] propertyFiles) {
            super("Duplicate key found in default property file "
                    + propertyFile
                    + ". Check that all default property files contains unique keys. Files: "
                    + Arrays.toString(propertyFiles));
        }
    }

}
