package com.kixeye.chassis.transport.http;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.annotation.Nullable;

public class ListenableFutureReturnValueHandler implements HandlerMethodReturnValueHandler {
    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Class<?> clazz = returnType.getParameterType();
        return ListenableFuture.class.isAssignableFrom(clazz);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }
        final DeferredResult<Object> deferredResult = new DeferredResult<>();
        Futures.addCallback((ListenableFuture<?>) returnValue, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
                deferredResult.setResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        });
        WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult,mavContainer);
    }
}
