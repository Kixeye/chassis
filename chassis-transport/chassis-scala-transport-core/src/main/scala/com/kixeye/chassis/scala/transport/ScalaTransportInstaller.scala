package com.kixeye.chassis.scala.transport

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import javax.annotation.PostConstruct
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import com.kixeye.chassis.scala.transport.http.ScalaFutureReturnValueHandler
import com.kixeye.chassis.transport.websocket.WebSocketAction
import com.kixeye.chassis.scala.transport.websocket.responseconverter.ScalaFutureWebSocketResponseConverter

/*
 * #%L
 * Chassis Scala Transport Core
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

@Component
class ScalaTransportInstaller @Autowired(required = false) (requestMappingHandlerAdapter: RequestMappingHandlerAdapter) {

  @PostConstruct
  def initialize() : Unit = {
    // Register a Scala Future HTTP response handler with SpringMVC
    if (requestMappingHandlerAdapter != null) {
      val handlers = new java.util.ArrayList[HandlerMethodReturnValueHandler](requestMappingHandlerAdapter.getReturnValueHandlers)
      handlers.add(0, new ScalaFutureReturnValueHandler())
      requestMappingHandlerAdapter.setReturnValueHandlers(handlers)
    }

    // Register a Scala Future converter with the web socket handler
    WebSocketAction.addWebSocketResponseConverter( new ScalaFutureWebSocketResponseConverter() )
  }

}
