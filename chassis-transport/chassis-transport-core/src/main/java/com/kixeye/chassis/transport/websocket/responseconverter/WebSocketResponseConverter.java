package com.kixeye.chassis.transport.websocket.responseconverter;

import org.springframework.web.context.request.async.DeferredResult;

public interface WebSocketResponseConverter {
    boolean canConvertResponse(Object response);
    DeferredResult<?> convertToDeferredResult(Object response);
}
