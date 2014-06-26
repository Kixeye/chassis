package com.kixeye.chassis.transport.http;

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
