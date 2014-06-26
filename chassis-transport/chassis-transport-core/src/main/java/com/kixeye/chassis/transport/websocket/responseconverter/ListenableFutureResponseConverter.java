package com.kixeye.chassis.transport.websocket.responseconverter;

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
