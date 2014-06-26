package com.kixeye.chassis.transport.websocket;

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

import org.springframework.core.MethodParameter;

/**
 * Used to resolve arguments on WebSocket {@link ActionMapping} annotated methods.  This
 * allows you to apply custom logic to resolve method arguments into java types.
 *
 * This is typically used convert {@link WebSocketEnvelope} headers or {@link WebSocketSession} properties
 * into java types during method invocation.
 *
 * Example:
 * @ActionMapping("my_websocket_action")
 * public void createUser(org.myorg.myapp.SecurityToken securityToken,...){
 *     ...
 * }
 *
 * public class SecurityTokenArgumentResolver implements WebSocketActionArgumentResolver {
 *
 *    public boolean supportsParameter(MethodParameter parameter){
 *        return parameter.getParameterType() == SecurityToken.class;
 *    }
 *
 *    public Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session){
 *
 *        //extract the security token from a custom header
 *        String token = envelope.getHeaders().get("security_token").get(0);
 *
 *        //logic to decrypt and validate the token
 *        String decrypted = decryptAndValidate(token);
 *
 *        //construct and return the SecurityToken
 *        return new SecurityToken(decrypted);
 *    }
 * }
 *
 * In the example above, lets pretend that every message must include an encrypted security token to be send as a header.
 * This could be implemented by using custom envelope header ("security_token" in this case).
 * SecurityTokenArgumentResolver performs decryption, validation and construction of the
 * SecurityToken object.  Upon invocation, the "createUser(SecurityToken, ...)" method will be able to use the validated SecurityToken object.
 *
 *
 * @author dturner@kixeye.com
 */
public interface WebSocketActionArgumentResolver {

    boolean supportsParameter(MethodParameter parameter);

    Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session);
}
