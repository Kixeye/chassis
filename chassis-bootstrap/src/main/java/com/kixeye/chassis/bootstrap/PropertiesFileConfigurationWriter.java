package com.kixeye.chassis.bootstrap;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kixeye.chassis.bootstrap.utils.ConfigurationUtils;

/**
 * Writes a configuration to a properties file
 *
 * @author dturner@kixeye.com
 */
public class PropertiesFileConfigurationWriter implements ConfigurationWriter{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractConfigsProcess.class);

    private String outputFile;

    public PropertiesFileConfigurationWriter(String outputFile){
        this.outputFile = outputFile;
    }

    @Override
    public void write(Configuration configuration, PropertyFilter filter) {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            dump(ConfigurationUtils.copy(configuration), filter, new PrintStream(fos));
        } catch (Exception e) {
            LOGGER.error("Unable to write configs to file {}", outputFile);
        }
    }

    private void dump(Map<String, ?> configuration, PropertyFilter filter, PrintStream printStream) {
        for(Map.Entry<String,?> entry:configuration.entrySet()){
            if(filter != null && filter.excludeProperty(entry.getKey(), entry.getValue())){
                continue;
            }
            printStream.println(entry.getKey()+"="+entry.getValue());
        }
    }
}
