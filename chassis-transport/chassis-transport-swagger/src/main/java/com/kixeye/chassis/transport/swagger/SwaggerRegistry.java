package com.kixeye.chassis.transport.swagger;

/*
 * #%L
 * Chassis Transport Swagger
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

import com.mangofactory.swagger.controllers.DefaultSwaggerController;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//import com.mangofactory.swagger.spring.controller.DocumentationController;

/**
 * Registers swagger with Jetty.
 * 
 * @author ebahtijaragic
 */
@Component
public class SwaggerRegistry {
	public static final String DEFAULT_SWAGGER_CONTEXT_PATH = "/swagger/*";
	
	/**
	 * Registers Swagger with default context.
	 */
	public void registerSwagger(ServletContextHandler context) {
		registerSwagger(context, DEFAULT_SWAGGER_CONTEXT_PATH);
	}
	
	/**
	 * Registers Swagger with context.
	 */
	public void registerSwagger(ServletContextHandler context, String swaggerContextPath) {
        context.addServlet(new ServletHolder(new SwaggerServlet(swaggerContextPath)), swaggerContextPath);
	}
}
