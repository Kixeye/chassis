package com.kixeye.chassis.scala.transport.websocket.responseconverter

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

import scala.concurrent.Future
import org.springframework.web.context.request.async.DeferredResult
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import com.kixeye.chassis.transport.websocket.responseconverter.WebSocketResponseConverter

class ScalaFutureWebSocketResponseConverter extends WebSocketResponseConverter {
  def canConvertResponse(response:AnyRef) : Boolean = {
    classOf[Future[_]].isAssignableFrom(response.getClass)
  }

  def convertToDeferredResult(response:AnyRef) : DeferredResult[Any] = {
    val deferredResult = new DeferredResult[Any]()
    val future:Future[_] = response.asInstanceOf[Future[_]]
    future onComplete {
      case Success(args) => deferredResult.setResult(args)
      case Failure(t) => deferredResult.setErrorResult(t)
    }
    deferredResult
  }
}
