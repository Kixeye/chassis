package com.kixeye.chassis.bootstrap.configuration.file;

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

import com.kixeye.chassis.bootstrap.configuration.ConfigurationWriter;
import com.kixeye.chassis.bootstrap.configuration.Configurations;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;

/**
 * Writes a configuration to a properties file
 *
 * @author dturner@kixeye.com
 */
public class PropertiesFileConfigurationWriter implements ConfigurationWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesFileConfigurationWriter.class);

    private String outputFile;

    public PropertiesFileConfigurationWriter(String outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void write(Configuration configuration, Filter filter) {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            dump(Configurations.asMap(configuration), filter, new PrintStream(fos));
        } catch (Exception e) {
            LOGGER.error("Unable to write configs to file {}", outputFile);
        }
    }

    private void dump(Map<String, ?> configuration, Filter filter, PrintStream printStream) {
        for (Map.Entry<String, ?> entry : configuration.entrySet()) {
            if (filter != null && filter.excludeProperty(entry.getKey(), entry.getValue())) {
                continue;
            }
            printStream.println(entry.getKey() + "=" + entry.getValue());
        }
    }
}
