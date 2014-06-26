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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Properties servlet.
 * 
 * @author ebahtijaragic
 */
public class ClasspathDumpServlet extends HttpServlet {
	private static final long serialVersionUID = 1694249128600231975L;
	
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        URLClassLoader loader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        for (URL url : loader.getURLs() ) {
        	 resp.getWriter().append( url.getPath() );
        	 resp.getWriter().append( System.lineSeparator() );
        }
    }
}
