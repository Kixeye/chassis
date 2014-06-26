package com.kixeye.chassis.bootstrap;

/**
 * Filters out system properties and internal bootstrap defined properties.
 *
 * @author dturner@kixeye.com
 */
public class DefaultPropertyFilter implements PropertyFilter{

    @Override
    public boolean excludeProperty(String key, Object value) {
        //exclude system props
        if (System.getProperty(key) != null) {
            return true;
        }
        //exclude internal bootstrap keys
        if (BootstrapConfigKeys.fromPropertyName(key) != null) {
            return true;
        }
        return false;
    }
}
