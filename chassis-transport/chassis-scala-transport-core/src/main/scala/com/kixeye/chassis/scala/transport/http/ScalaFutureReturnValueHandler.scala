package com.kixeye.chassis.scala.transport.http

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

import org.springframework.web.method.support.{ModelAndViewContainer, HandlerMethodReturnValueHandler}
import org.springframework.core.MethodParameter
import scala.concurrent.Future
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.async.{WebAsyncUtils, DeferredResult}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class ScalaFutureReturnValueHandler extends HandlerMethodReturnValueHandler {
  def supportsReturnType(returnType:MethodParameter) : Boolean = {
    val clazz = returnType.getParameterType
    classOf[Future[_]].isAssignableFrom(clazz)
  }

  def handleReturnValue(returnValue:AnyRef, returnType:MethodParameter, mavContainer:ModelAndViewContainer, webRequest:NativeWebRequest) : Unit = {
    if (returnType == null) {
      mavContainer.setRequestHandled(true)
      return
    }
    val deferredResult = new DeferredResult[Any]()
    val future:Future[_] = returnValue.asInstanceOf[Future[_]]
    future onComplete {
      case Success(args) => deferredResult.setResult(args)
      case Failure(t) => deferredResult.setErrorResult(t)
    }
    WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult, mavContainer)
  }
}
