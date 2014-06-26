package com.kixeye.chassis.transport.websocket.responseconverter;

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
