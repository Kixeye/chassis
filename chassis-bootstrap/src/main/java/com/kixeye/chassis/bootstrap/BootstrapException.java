package com.kixeye.chassis.bootstrap;

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

    public static void configurationMissingFromZookeeper(String zookeeperHost, String path, Exception e) {
        throw new MissingApplicationConfigurationInZookeeperException(zookeeperHost, path, e);
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

    public static class MissingApplicationConfigurationInZookeeperException extends BootstrapException {
        public MissingApplicationConfigurationInZookeeperException(String zookeeperHost, String path, Exception e) {
            super("Unable to verify Zookeeper configuration path " + path +
                    ". Zookeeper configuration path format is /{environment}/{app name}/{app version}. Please verify that you zookeeper (" +
                    zookeeperHost + ") has that path.", e);
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
