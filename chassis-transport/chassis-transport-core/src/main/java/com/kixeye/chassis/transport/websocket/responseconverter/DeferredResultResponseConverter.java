package com.kixeye.chassis.transport.websocket.responseconverter;

import org.springframework.web.context.request.async.DeferredResult;

public class DeferredResultResponseConverter implements WebSocketResponseConverter {

    @Override
    public boolean canConvertResponse(Object response) {
        return DeferredResult.class.isAssignableFrom(response.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeferredResult<Object> convertToDeferredResult(Object response) {
        return (DeferredResult<Object>) response;
    }
}
