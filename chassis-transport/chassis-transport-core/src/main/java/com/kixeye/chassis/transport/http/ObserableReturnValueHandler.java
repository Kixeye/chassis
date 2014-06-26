package com.kixeye.chassis.transport.http;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import rx.Observable;
import rx.Observer;

public class ObserableReturnValueHandler implements HandlerMethodReturnValueHandler {

    private static final Logger logger = LoggerFactory.getLogger(ObserableReturnValueHandler.class);

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Class<?> clazz = returnType.getParameterType();
        return Observable.class.isAssignableFrom(clazz);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }
        final DeferredResult<Object> deferredResult = new DeferredResult<>();
        ((Observable<?>) returnValue).subscribe( new Observer<Object>() {
            @Override
            public void onCompleted() {
                if (!deferredResult.hasResult()) {
                    logger.error( "onComplete before onNext");
                    deferredResult.setResult(null);
                }
            }

            @Override
            public void onError(Throwable e) {
                deferredResult.setErrorResult(e);
            }

            @Override
            public void onNext(Object args) {
                deferredResult.setResult(args);
            }
        });
        WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult,mavContainer);
    }
}
