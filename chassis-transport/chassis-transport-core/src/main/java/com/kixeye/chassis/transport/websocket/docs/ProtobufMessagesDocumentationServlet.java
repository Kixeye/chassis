package com.kixeye.chassis.transport.websocket.docs;

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
