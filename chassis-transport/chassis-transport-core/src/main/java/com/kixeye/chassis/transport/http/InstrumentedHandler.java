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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicProperty;
import com.netflix.config.DynamicPropertyFactory;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.AsyncContextState;
import org.eclipse.jetty.server.HttpChannelState;
import org.eclipse.jetty.server.HttpChannelState.State;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * A copy of the InstrumentedHandler from Codahale Metric's library.  The current version of the library
 * does not support the most current Jetty version, in which HttpChannelState.isDispatched() has been removed.
 * This class will be used until a Metric's library version is released which supports the Jetty change.
 */
public class InstrumentedHandler extends HandlerWrapper {
    private final MetricRegistry metricRegistry;

    private String name;

    // the requests handled by this handler, excluding active
    private Timer requests;

    // the number of dispatches seen by this handler, excluding active
    private Timer dispatches;

    // the number of active requests
    private Counter activeRequests;

    // the number of active dispatches
    private Counter activeDispatches;

    // the number of requests currently suspended.
    private Counter activeSuspended;

    // the number of requests that have been asynchronously dispatched
    private Meter asyncDispatches;

    // the number of requests that expired while suspended
    private Meter asyncTimeouts;

    private Meter[] responses;

    private Timer getRequests;
    private Timer postRequests;
    private Timer headRequests;
    private Timer putRequests;
    private Timer deleteRequests;
    private Timer optionsRequests;
    private Timer traceRequests;
    private Timer connectRequests;
    private Timer moveRequests;
    private Timer otherRequests;

    private AsyncListener listener;

    private DynamicLongProperty timerReservoirSeconds = DynamicPropertyFactory.getInstance().getLongProperty("http.metrics.handler.timerReservoirSeconds", 60);

    /**
     * Create a new instrumented handler using a given metrics registry.
     *
     * @param registry the registry for the metrics
     */
    public InstrumentedHandler(MetricRegistry registry) {
        this.metricRegistry = registry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final String prefix = name(getHandler().getClass(), name);

        this.requests = timer(name(prefix, "requests"));
        this.dispatches = timer(name(prefix, "dispatches"));

        this.activeRequests = metricRegistry.counter(name(prefix, "active-requests"));
        this.activeDispatches = metricRegistry.counter(name(prefix, "active-dispatches"));
        this.activeSuspended = metricRegistry.counter(name(prefix, "active-suspended"));

        this.asyncDispatches = metricRegistry.meter(name(prefix, "async-dispatches"));
        this.asyncTimeouts = metricRegistry.meter(name(prefix, "async-timeouts"));

        this.responses = new Meter[]{
                metricRegistry.meter(name(prefix, "1xx-responses")), // 1xx
                metricRegistry.meter(name(prefix, "2xx-responses")), // 2xx
                metricRegistry.meter(name(prefix, "3xx-responses")), // 3xx
                metricRegistry.meter(name(prefix, "4xx-responses")), // 4xx
                metricRegistry.meter(name(prefix, "5xx-responses"))  // 5xx
        };

        this.getRequests = timer(name(prefix, "get-requests"));
        this.postRequests = timer(name(prefix, "post-requests"));
        this.headRequests = timer(name(prefix, "head-requests"));
        this.putRequests = timer(name(prefix, "put-requests"));
        this.deleteRequests = timer(name(prefix, "delete-requests"));
        this.optionsRequests = timer(name(prefix, "options-requests"));
        this.traceRequests = timer(name(prefix, "trace-requests"));
        this.connectRequests = timer(name(prefix, "connect-requests"));
        this.moveRequests = timer(name(prefix, "move-requests"));
        this.otherRequests = timer(name(prefix, "other-requests"));

        this.listener = new AsyncListener() {
            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                asyncTimeouts.mark();
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                event.getAsyncContext().addListener(this);
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
            }

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                final AsyncContextState state = (AsyncContextState) event.getAsyncContext();
                final Request request = (Request) state.getRequest();
                updateResponses(request);
                if (!(state.getHttpChannelState().getState() == State.DISPATCHED)) {
                    activeSuspended.dec();
                }
            }
        };
    }

    private Timer timer(String name) {
        Timer timer = metricRegistry.getTimers().get(name);
        if (timer != null) {
            return timer;
        }
        try {
            return metricRegistry.register(name, new Timer(new SlidingTimeWindowReservoir(timerReservoirSeconds.get(), TimeUnit.SECONDS)));
        } catch (IllegalArgumentException e) {
            //timer already exists. this happens due to race condition. its fine.
            return metricRegistry.getTimers().get(name);
        }
    }

    @Override
    public void handle(String path,
                       Request request,
                       HttpServletRequest httpRequest,
                       HttpServletResponse httpResponse) throws IOException, ServletException {

        activeDispatches.inc();

        final long start;
        final HttpChannelState state = request.getHttpChannelState();
        if (state.isInitial()) {
            // new request
            activeRequests.inc();
            start = request.getTimeStamp();
        } else {
            // resumed request
            start = System.currentTimeMillis();
            activeSuspended.dec();
            if (state.getState() == State.DISPATCHED) {
                asyncDispatches.mark();
            }
        }

        try {
            super.handle(path, request, httpRequest, httpResponse);
        } finally {
            final long now = System.currentTimeMillis();
            final long dispatched = now - start;

            activeDispatches.dec();
            dispatches.update(dispatched, TimeUnit.MILLISECONDS);

            if (state.isSuspended()) {
                if (state.isInitial()) {
                    state.addListener(listener);
                }
                activeSuspended.inc();
            } else if (state.isInitial()) {
                requests.update(dispatched, TimeUnit.MILLISECONDS);
                updateResponses(request);
            }
            // else onCompletion will handle it.
        }
    }

    private Timer requestTimer(String method) {
        final HttpMethod m = HttpMethod.fromString(method);
        if (m == null) {
            return otherRequests;
        } else {
            switch (m) {
                case GET:
                    return getRequests;
                case POST:
                    return postRequests;
                case PUT:
                    return putRequests;
                case HEAD:
                    return headRequests;
                case DELETE:
                    return deleteRequests;
                case OPTIONS:
                    return optionsRequests;
                case TRACE:
                    return traceRequests;
                case CONNECT:
                    return connectRequests;
                case MOVE:
                    return moveRequests;
                default:
                    return otherRequests;
            }
        }
    }

    private void updateResponses(Request request) {
        final int response = request.getResponse().getStatus() / 100;
        if (response >= 1 && response <= 5) {
            responses[response - 1].mark();
        }
        activeRequests.dec();
        final long elapsedTime = System.currentTimeMillis() - request.getTimeStamp();
        requests.update(elapsedTime, TimeUnit.MILLISECONDS);
        requestTimer(request.getMethod()).update(elapsedTime, TimeUnit.MILLISECONDS);
    }
}
