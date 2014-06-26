package com.kixeye.chassis.transport.shared;

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
