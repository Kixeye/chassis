package com.kixeye.chassis.transport.websocket;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import java.lang.reflect.Method;

/**
 * Unit tests for {@link WebSocketAction}
 *
 * @author dturner@kixeye.com
 */
public class WebSocketActionTest {

    @Test
    public void testActionWithSingleCustomArgResolver() throws Exception {
        Method method = WebSocketActionTest.class.getMethod("handler", String.class);
        WebSocketActionArgumentResolver arg1Resolver = new WebSocketActionArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterIndex() == 0;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session) {
                return "1";
            }
        };
        WebSocketAction action = new WebSocketAction(method, null, arg1Resolver);

        RawWebSocketMessage message = Mockito.mock(RawWebSocketMessage.class);
        WebSocketEnvelope envelope = Mockito.mock(WebSocketEnvelope.class);
        WebSocketSession session = Mockito.mock(WebSocketSession.class);

        Assert.assertEquals("1", action.invoke(this, message, envelope, session).getResult());
    }

    @Test
    public void testActionWithSingleCustomArgResolver_reservedParamType_webSocketSession() throws Exception {
        Method method = WebSocketActionTest.class.getMethod("handler", WebSocketSession.class, WebSocketEnvelope.class, String.class, String.class);
        WebSocketActionArgumentResolver arg1Resolver = new WebSocketActionArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType() == WebSocketSession.class;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session) {
                return "1";
            }
        };
        WebSocketAction action = new WebSocketAction(method, null, arg1Resolver);

        RawWebSocketMessage message = Mockito.mock(RawWebSocketMessage.class);
        WebSocketEnvelope envelope = Mockito.mock(WebSocketEnvelope.class);
        WebSocketSession session = Mockito.mock(WebSocketSession.class);

        Assert.assertNull(action.invoke(this, message, envelope, session));
    }

    @Test
    public void testActionWithSingleCustomArgResolverAndWebSocketSession() throws Exception {
        Method method = WebSocketActionTest.class.getMethod("handler", WebSocketSession.class, String.class);
        WebSocketActionArgumentResolver arg1Resolver = new WebSocketActionArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterIndex() == 1;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session) {
                return "1";
            }
        };
        WebSocketAction action = new WebSocketAction(method, null, arg1Resolver);

        RawWebSocketMessage message = Mockito.mock(RawWebSocketMessage.class);
        WebSocketEnvelope envelope = Mockito.mock(WebSocketEnvelope.class);
        WebSocketSession session = Mockito.mock(WebSocketSession.class);

        Assert.assertEquals("1", action.invoke(this, message, envelope, session).getResult());
    }

    @Test
    public void testActionWithCustomArgResolver() throws Exception {
        Method method = WebSocketActionTest.class.getMethod("handler", String.class, String.class);
        WebSocketActionArgumentResolver arg1Resolver = new WebSocketActionArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterIndex() == 0;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session) {
                return "1";
            }
        };
        WebSocketActionArgumentResolver arg2Resolver = new WebSocketActionArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterIndex() == 1;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session) {
                return "2";
            }
        };
        WebSocketAction action = new WebSocketAction(method, null, arg1Resolver, arg2Resolver);

        RawWebSocketMessage message = Mockito.mock(RawWebSocketMessage.class);
        WebSocketEnvelope envelope = Mockito.mock(WebSocketEnvelope.class);
        WebSocketSession session = Mockito.mock(WebSocketSession.class);

        Assert.assertEquals("12", action.invoke(this, message, envelope, session).getResult());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testActionWithCustomArgResolver_multipleForSameArg() throws Exception {
        Method method = WebSocketActionTest.class.getMethod("handler", String.class, String.class);
        WebSocketActionArgumentResolver arg1Resolver = new WebSocketActionArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterIndex() == 0;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session) {
                return "1";
            }
        };
        WebSocketActionArgumentResolver arg2Resolver = new WebSocketActionArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterIndex() == 0;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, WebSocketEnvelope envelope, WebSocketSession session) {
                return "2";
            }
        };
        WebSocketAction action = new WebSocketAction(method, null, arg1Resolver, arg2Resolver);

        Assert.fail();
    }

    public String handler(String arg1) {
        return arg1;
    }

    public String handler(WebSocketSession session, String arg1) {
        return arg1;
    }

    public String handler(WebSocketSession session, WebSocketEnvelope envelope, @ActionTransactionId String transactionId, String custom) {
        return custom;
    }

    public String handler(String arg1, String arg2) {
        return arg1 + arg2;
    }
}
