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

import org.springframework.web.context.request.async.DeferredResult;
import rx.Observable;
import rx.Observer;

public class ObservableResponseConverter implements WebSocketResponseConverter {

    @Override
    public boolean canConvertResponse(Object response) {
        return Observable.class.isAssignableFrom(response.getClass());
    }

    @Override
    public DeferredResult<Object> convertToDeferredResult(Object response) {
        final DeferredResult<Object> deferredResult = new DeferredResult<>();
        ((Observable<?>) response).subscribe( new Observer<Object>() {
            @Override
            public void onCompleted() {
                if (!deferredResult.hasResult()) {
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
        return deferredResult;
    }
}
