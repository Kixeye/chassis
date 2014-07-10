package com.kixeye.chassis.scala.transport
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

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import javax.annotation.PostConstruct
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import com.kixeye.chassis.scala.transport.http.ScalaFutureReturnValueHandler
import com.kixeye.chassis.transport.websocket.WebSocketAction
import com.kixeye.chassis.scala.transport.websocket.responseconverter.ScalaFutureWebSocketResponseConverter
import com.kixeye.chassis.transport.util.SpringContextWrapper
import org.springframework.context.ApplicationContext


/**
 * Registers a ScalaFutureReturnValueHandler with spring mvc framework
 */
@Component
class ScalaTransportInstaller @Autowired(required = false) (applicationContext: ApplicationContext) {

  @PostConstruct
  def initialize() : Unit = {
    // Register a Scala Future HTTP response handler with SpringMVC

    registerReturnValueHandler(applicationContext)

    // in chassis, spring mvc context is registered as a child context of the main chassis context.  the child
    // context is exposed to the parent via the SpringContextWrapper. If found, register the handler with
    // the RequestMappingHandlerAdapter of the child.
    val childContext = getBeanOfTypeQuietly(classOf[SpringContextWrapper], applicationContext)
    childContext.foreach(wrapper => registerReturnValueHandler(wrapper.getContext))

    // Register a Scala Future converter with the web socket handler
    WebSocketAction.addWebSocketResponseConverter( new ScalaFutureWebSocketResponseConverter() )
  }

  def registerReturnValueHandler(applicationContext: ApplicationContext) : Unit = {
      registerReturnValueHandler(getBeanOfTypeQuietly(classOf[RequestMappingHandlerAdapter], applicationContext))
  }

  def registerReturnValueHandler(requestMappingHandlerAdapterOption: Option[RequestMappingHandlerAdapter]) : Unit = {
    if(requestMappingHandlerAdapterOption.isEmpty){
      return
    }
    val requestMappingHandlerAdapter = requestMappingHandlerAdapterOption.get
    val handlers = new java.util.ArrayList[HandlerMethodReturnValueHandler](requestMappingHandlerAdapter.getReturnValueHandlers)
    handlers.add(0, new ScalaFutureReturnValueHandler())
    requestMappingHandlerAdapter.setReturnValueHandlers(handlers)
  }

  def getBeanOfTypeQuietly [T] (beanClass: Class[T], applicationContext: ApplicationContext): Option[T] = {
    val t = applicationContext.getBeansOfType(beanClass)
    if(t.isEmpty){
      return Option.empty
    }
    Option(t.values().iterator().next())
  }

}
