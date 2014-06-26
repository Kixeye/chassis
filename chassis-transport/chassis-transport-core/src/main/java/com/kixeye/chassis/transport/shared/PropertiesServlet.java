package com.kixeye.chassis.transport.shared;

/*
 * #%L
 * Chassis Transport Core
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

import com.netflix.config.ConfigurationManager;
import org.apache.commons.configuration.AbstractConfiguration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Properties servlet.
 * 
 * @author ebahtijaragic
 */
public class PropertiesServlet extends HttpServlet {
	private static final long serialVersionUID = 1694249128600231975L;
	
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get list of properties
        TreeSet<String> properties = new TreeSet<String>();
    	AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        Iterator<String> keys = config.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = config.getProperty(key);
            if ("aws.accessId".equals(key)
                    || "aws.secretKey".equals(key)
                    || "experiments-service.secret".equals(key)
                    || "java.class.path".equals(key)
                    || key.contains("framework.securityDefinition")
                    || key.contains("password")
                    || key.contains("secret")) {
                value = "*****";
            }
            properties.add(key + "=" + value.toString());
        }

        // write them out in sorted order
        for (String line : properties) {
            resp.getWriter().append(line).println();
        }
    }
}
