package com.kixeye.chassis.transport.websocket.docs;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kixeye.chassis.transport.websocket.WebSocketAction;
import com.kixeye.chassis.transport.websocket.WebSocketMessageMappingRegistry;
import com.kixeye.chassis.transport.websocket.WebSocketMessageRegistry;

/**
 * Generates protobuf schema.
 * 
 * @author ebahtijaragic
 */
public class ProtobufMessagesDocumentationServlet extends HttpServlet {
	private static final long serialVersionUID = -6084037269570869713L;
	
	private WebSocketMessageRegistry messageRegistry;
	private WebSocketMessageMappingRegistry mappingRegistry;
	private String appName;
	
	public ProtobufMessagesDocumentationServlet(String appName, WebSocketMessageMappingRegistry mappingRegistry, WebSocketMessageRegistry messageRegistry) {
		this.appName = appName;
		this.mappingRegistry = mappingRegistry;
		this.messageRegistry = messageRegistry;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Map<String, String> schemas = new HashMap<String, String>();

		try {
			for (String typeId : messageRegistry.getTypeIds()) {
				ProtobufSchemaGenerator.generateSchema(messageRegistry.getClassByTypeId(typeId), schemas, messageRegistry);
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		resp.getWriter().println("package " + appName + ";");
		resp.getWriter().println();
		
		for (String message : schemas.values()) {
			resp.getWriter().println(message);
		}
		
		resp.getWriter().append("service " + appName + " {").println();
		for (String action : mappingRegistry.getActions()) {
			Collection<WebSocketAction> actionMethods = mappingRegistry.getActionMethods(action);
			
			for (WebSocketAction actionMethod : actionMethods) {
				String requestType = actionMethod.getPayloadClass() == null ? "" : messageRegistry.getTypeIdByClass(actionMethod.getPayloadClass());
				String responseType = actionMethod.getResponseClass() == null ? "" : messageRegistry.getTypeIdByClass(actionMethod.getResponseClass());
				
				resp.getWriter().append("\t").append("rpc ").append(action).append("(").append(requestType).append(")").append(" returns (").append(responseType).append(");").println();
			}
		}
		resp.getWriter().append("}").println();
	}
}
