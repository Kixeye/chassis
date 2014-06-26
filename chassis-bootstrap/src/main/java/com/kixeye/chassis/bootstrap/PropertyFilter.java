package com.kixeye.chassis.bootstrap;

/**
 * Used to filter out properties
 *
 * @author dturner@kixeye.com
 */
public interface PropertyFilter {

    /**
     * whether or not to exclude the given property
     * @param key
     * @param value
     * @return
     */
    public boolean excludeProperty(String key, Object value);
}
