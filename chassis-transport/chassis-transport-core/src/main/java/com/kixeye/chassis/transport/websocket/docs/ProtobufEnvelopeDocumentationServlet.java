package com.kixeye.chassis.transport.websocket.docs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kixeye.chassis.transport.dto.Envelope;

/**
 * Generates protobuf envelope schema.
 * 
 * @author ebahtijaragic
 */
public class ProtobufEnvelopeDocumentationServlet extends HttpServlet {
	private static final long serialVersionUID = 5397593236087739762L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Map<String, String> schemas = new HashMap<String, String>();
		
		try {
			ProtobufSchemaGenerator.generateSchema(Envelope.class, schemas, null);
		} catch (Exception e) {
			throw new ServletException(e);
		}

		resp.getWriter().println("package Transport;");
		resp.getWriter().println();
		
		for (String message : schemas.values()) {
			resp.getWriter().println(message);
		}
	}
}
