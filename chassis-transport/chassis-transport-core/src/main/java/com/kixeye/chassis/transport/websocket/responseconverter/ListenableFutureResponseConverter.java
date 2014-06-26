package com.kixeye.chassis.transport.websocket.responseconverter;

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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Nullable;


public class ListenableFutureResponseConverter implements WebSocketResponseConverter {
    @Override
    public boolean canConvertResponse(Object response) {
        return ListenableFuture.class.isAssignableFrom(response.getClass());
    }

    @Override
    public DeferredResult<Object> convertToDeferredResult(Object response) {
        final DeferredResult<Object> deferredResult = new DeferredResult<>();
        Futures.addCallback((ListenableFuture<?>) response, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
                deferredResult.setResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        });
        return deferredResult;
    }
}
