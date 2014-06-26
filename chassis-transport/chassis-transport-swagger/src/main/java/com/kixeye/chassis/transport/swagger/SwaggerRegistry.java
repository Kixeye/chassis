package com.kixeye.chassis.transport.swagger;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mangofactory.swagger.spring.controller.DocumentationController;

/**
 * Registers swagger with Jetty.
 * 
 * @author ebahtijaragic
 */
@Component
public class SwaggerRegistry {
	public static final String DEFAULT_SWAGGER_CONTEXT_PATH = "/swagger/*";
	
	@Autowired
	private DocumentationController documentationController;
	
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
		documentationController.setServletContext(context.getServletContext());

        context.addServlet(new ServletHolder(new SwaggerServlet(swaggerContextPath)), swaggerContextPath);
	}
}
