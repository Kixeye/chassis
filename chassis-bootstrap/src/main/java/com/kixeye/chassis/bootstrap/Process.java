package com.kixeye.chassis.bootstrap;

/**
 * Runs an application
 *
 * @author dturner@kixeye.com
 */
public interface Process {
    void run();

    public enum Type{
        SERVER,
        EXTRACT_CONFIGS
    }
}
