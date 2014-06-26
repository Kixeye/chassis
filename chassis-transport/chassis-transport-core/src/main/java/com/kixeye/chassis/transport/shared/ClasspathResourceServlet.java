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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * A servlet to load resources.
 * 
 * @author ebahtijaragic
 */
public class ClasspathResourceServlet extends HttpServlet {
	private static final long serialVersionUID = -4202695753188111009L;
	
	private DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
	private String classPathDirectory;
	
	/**
	 * Gets the parent directory.
	 * 
	 * @param classPathDirectory
	 */
	public ClasspathResourceServlet(String classPathDirectory) {
		this.classPathDirectory = classPathDirectory;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Resource resource = resourceLoader.getResource("classpath:" + classPathDirectory + req.getPathInfo());
		
		if (resource.exists()) {
			StreamUtils.copy(resource.getInputStream(), resp.getOutputStream());
			resp.setStatus(HttpServletResponse.SC_OK);
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
