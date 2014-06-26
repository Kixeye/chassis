package com.kixeye.chassis.bootstrap;

import org.apache.commons.configuration.Configuration;

/**
 * Writes a configuration
 *
 * @author dturner@kixeye.com
 */
public interface ConfigurationWriter {

    void write(Configuration configuration, PropertyFilter filter);

}
