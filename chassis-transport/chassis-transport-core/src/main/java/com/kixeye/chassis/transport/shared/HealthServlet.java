package com.kixeye.chassis.transport.shared;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * Admin servlet.
 * 
 * @author ebahtijaragic
 */
public class HealthServlet extends HttpServlet {
	private static final long serialVersionUID = 1694249128600231975L;
	
    private HealthCheckRegistry healthCheckRegistry;
    
    public HealthServlet(HealthCheckRegistry healthCheckRegistry) {
    	this.healthCheckRegistry = healthCheckRegistry;
    }
	
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	if (healthCheckRegistry != null) {
            for (Map.Entry<String, HealthCheck.Result> entry : healthCheckRegistry.runHealthChecks().entrySet()) {
                if (!entry.getValue().isHealthy()) {
                	resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                	resp.getWriter().write(entry.getKey());
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        	resp.getWriter().write("OK");
        } else {
        	resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        	resp.getWriter().write("No health check registry");
        }
    }
}
