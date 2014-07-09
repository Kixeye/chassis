package com.kixeye.chassis.chassis.test.metrics.cloudwatch;

/*
 * #%L
 * Chassis Support
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.io.Closeables;
import com.kixeye.chassis.chassis.metrics.aws.CloudWatchFactory;
import com.kixeye.chassis.chassis.metrics.aws.MetricsCloudWatchReporter;

/**
 * Unit test for MetricsCloudWatchRepoter
 *
 * @author dturner@kixeye.com
 */
public class CloudWatchReporterTest {
    private static final String APP_NAME = "UnitTestApp";
    private static final String APP_ENVIRONMENT = "UnitTestEnvironment";
    private static final String APP_VERSION = "UnitTestVersion";

    private MetricsCloudWatchReporter reporter;

    @After
    public void after() throws IOException {
        if (reporter != null) {
            Closeables.close(reporter, true);
        }
    }

    /**
     * successfully publish metrics. no stat filtering
     */
    @Test
    public void testPublishMetrics() throws InterruptedException {

        MetricRegistry metricRegistry = new MetricRegistry();

        metricRegistry.counter("UnitTestCounter1").inc();
        metricRegistry.counter("UnitTestCounter2").inc();
        metricRegistry.counter("UnitTestCounter2").inc();
        metricRegistry.counter("UnitTestCounter3").inc();
        metricRegistry.meter("UnitTestMeter");
        metricRegistry.histogram("UnitTestHistogram");
        metricRegistry.timer("UnitTestTimer");
        metricRegistry.register("UnitTestGauge", new Gauge<Object>() {
            @Override
            public Object getValue() {
                return 1;
            }
        });

        //this gauge should not be reported to AWS because its value is not numeric
        metricRegistry.register("InvalidUnitTestGauge", new Gauge<Object>() {
            @Override
            public Object getValue() {
                return "foo";
            }
        });

        final AmazonCloudWatch amazonCloudWatch = Mockito.mock(AmazonCloudWatch.class);

        reporter =
                new MetricsCloudWatchReporter(
                        APP_NAME,
                        APP_VERSION,
                        APP_ENVIRONMENT,
                        "utc1=UnitTestCounter1,utc2=UnitTestCounter2,utg=UnitTestGauge,utm=UnitTestMeter,uth=UnitTestHistogram,utt=UnitTestTimer",
                        2,
                        TimeUnit.SECONDS,
                        metricRegistry,
                        createCloudWatchFactory(amazonCloudWatch));
        reporter.start();

        //give the reporter a chance to publish
        Thread.sleep(3000);

        PutMetricDataRequestMatcher matcher = new PutMetricDataRequestMatcher(
                new MetricDatumValidator("utg", APP_ENVIRONMENT, 1d),
                new MetricDatumValidator("utc1", APP_ENVIRONMENT, 1d),
                new MetricDatumValidator("utc2", APP_ENVIRONMENT, 2d),

                new MetricDatumValidator("uth.count", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.min", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.max", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.mean", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.stddev", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.75p", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.95p", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.98p", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.99p", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("uth.999p", APP_ENVIRONMENT, 0d),

                new MetricDatumValidator("utm.1m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utm.5m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utm.15m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utm.mean", APP_ENVIRONMENT, 0d),

                new MetricDatumValidator("utt.count", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utt.1m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utt.5m", APP_ENVIRONMENT, 0d)
        );

        PutMetricDataRequestMatcher matcher2 =
                new PutMetricDataRequestMatcher(new MetricDatumValidator("utt.15m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utt.mean", APP_ENVIRONMENT, 0d)
        );

        //first request to AWS with 20 events
        Mockito.verify(amazonCloudWatch, Mockito.times(1)).putMetricData(Mockito.argThat(matcher));

        //seconds request to AWS with 2 events
        Mockito.verify(amazonCloudWatch, Mockito.times(1)).putMetricData(Mockito.argThat(matcher2));
    }

    /**
     * A metric is not fully filtered, but some stats within the metric are
     */
    @Test
    public void testPublishFilteredMetrics_metricStatFiltered() throws InterruptedException {
        MetricRegistry metricRegistry = new MetricRegistry();

        metricRegistry.meter("UnitTestMeter1").mark();
        metricRegistry.meter("UnitTestMeter2").mark();

        final AmazonCloudWatch amazonCloudWatch = Mockito.mock(AmazonCloudWatch.class);

        reporter =
                new MetricsCloudWatchReporter(
                        APP_NAME,
                        APP_VERSION,
                        APP_ENVIRONMENT,
                        "utm1=UnitTestMeter1,utm2=UnitTestMeter2:1m:5m:15m",
                        2,
                        TimeUnit.SECONDS,
                        metricRegistry,
                        createCloudWatchFactory(amazonCloudWatch));
        reporter.start();

        //give the reporter a chance to publish
        Thread.sleep(3000);

        PutMetricDataRequestMatcher matcher = new PutMetricDataRequestMatcher(
                new MetricDatumValidator("utm1.1m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utm1.5m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utm1.15m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utm1.mean", APP_ENVIRONMENT, null),
                new MetricDatumValidator("utm2.1m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utm2.5m", APP_ENVIRONMENT, 0d),
                new MetricDatumValidator("utm2.15m", APP_ENVIRONMENT, 0d));

        Mockito.verify(amazonCloudWatch, Mockito.times(1)).putMetricData(Mockito.argThat(matcher));
    }

    /**
     * Ensure that a failed publication does not prevent subsequent attempts
     */
    @Test
    @Ignore("Fix the thread sleeps.")
    public void testRecoverAfterFailedPublication() throws InterruptedException {
        MetricRegistry metricRegistry = new MetricRegistry();

        metricRegistry.counter("UnitTestCounter").inc();

        final AmazonCloudWatch amazonCloudWatch = Mockito.mock(AmazonCloudWatch.class);

        reporter =
                new MetricsCloudWatchReporter(
                        APP_NAME,
                        APP_VERSION,
                        APP_ENVIRONMENT,
                        "utc=UnitTestCounter",
                        2,
                        TimeUnit.SECONDS,
                        metricRegistry,
                        createCloudWatchFactory(amazonCloudWatch));

        Mockito.doThrow(new RuntimeException("CloudWatch request error")).when(amazonCloudWatch).putMetricData(Mockito.any(PutMetricDataRequest.class));

        reporter.start();

        //give the reporter a chance to publish
        Thread.sleep(3000);

        //verify that
        Mockito.verify(amazonCloudWatch, Mockito.times(1)).putMetricData(Mockito.any(PutMetricDataRequest.class));

        Mockito.reset(amazonCloudWatch);

        metricRegistry.counter("UnitTestCounter").inc();

        Thread.sleep(3000);

        PutMetricDataRequestMatcher matcher = new PutMetricDataRequestMatcher(
                new MetricDatumValidator("utc", APP_ENVIRONMENT, 2d));

        Mockito.verify(amazonCloudWatch, Mockito.times(2)).putMetricData(Mockito.argThat(matcher));
    }

    /**
     * Ensure all metrics are published event when the max number of cloudwatch metrics (per request) is exceeded.
     */
    @Test
    public void testPublishInMultipleCloudWatchRequests() throws InterruptedException {
        MetricRegistry metricRegistry = new MetricRegistry();

        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < MetricsCloudWatchReporter.MAX_CLOUDWATCH_DATUM_PER_REQUEST + 1; i++) {
            String metric = "UnitTestCounter" + i;
            metricRegistry.counter(metric).inc();
            if (i > 0) {
                filter.append(",");
            }
            filter.append(metric).append("=").append(metric);
        }

        final AmazonCloudWatch amazonCloudWatch = Mockito.mock(AmazonCloudWatch.class);

        reporter =
                new MetricsCloudWatchReporter(
                        APP_NAME,
                        APP_VERSION,
                        APP_ENVIRONMENT,
                        filter.toString(),
                        2,
                        TimeUnit.SECONDS,
                        metricRegistry,
                        createCloudWatchFactory(amazonCloudWatch));

        reporter.start();

        Mockito.verify(amazonCloudWatch, Mockito.never()).putMetricData(Mockito.any(PutMetricDataRequest.class));

        Thread.sleep(3000);

        Mockito.verify(amazonCloudWatch, Mockito.times(2)).putMetricData(Mockito.any(PutMetricDataRequest.class));
    }


    private CloudWatchFactory createCloudWatchFactory(final AmazonCloudWatch amazonCloudWatch) {
        return new CloudWatchFactory() {
            @Override
            public AmazonCloudWatch getCloudWatchClient() {
                return amazonCloudWatch;
            }
        };
    }

    private class MetricDatumValidator {
        private String name;
        private String environment;
        private Double value;

        private MetricDatumValidator(String name, String environment, Double value) {
            this.name = name;
            this.environment = environment;
            this.value = value;
        }

        void validate(MetricDatum datum) throws Exception {
            if (datum == null) {
                throw new Exception("No Data");
            }
            if (!datum.getMetricName().equals(name)) {
                throw new Exception("invalid metric name " + datum.getMetricName() + ". expected " + name);
            }
            if (value != null && !(Math.round(datum.getValue()) == Math.round(value))) {
                throw new Exception("invalid data value " + Math.round(datum.getValue()) + ". expected " + Math.round(value));
            }
            if (datum.getDimensions().size() != 1) {
                throw new Exception("expected 1 dimension, got " + datum.getDimensions().size());
            }
            if (!datum.getDimensions().get(0).getName().equals("environment")) {
                throw new Exception("invalid dimension name. expected \"environment\", got " + datum.getDimensions().get(0).getName());
            }
            if (!datum.getDimensions().get(0).getValue().equals(environment)) {
                throw new Exception("invalid dimension value " + datum.getDimensions().get(0).getValue() + ". expected " + environment);
            }
            if (datum.getTimestamp() == null) {
                throw new Exception("no timestamp");
            }
            long timeDelta = Math.abs(System.currentTimeMillis() - datum.getTimestamp().getTime());
            if (timeDelta > 5000) {
                throw new Exception("invalid timestamp. expected it to less than 5000 millis old. was " + timeDelta + " millis old.");
            }
        }
    }

    private class PutMetricDataRequestMatcher extends BaseMatcher<PutMetricDataRequest> {

        private String errorText;
        private MetricDatumValidator[] validators;

        private PutMetricDataRequestMatcher(MetricDatumValidator... validators) {
            this.validators = validators;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof PutMetricDataRequest)) {
                errorText = "Invalid arg type " + o;
                return false;
            }
            PutMetricDataRequest request = (PutMetricDataRequest) o;
            if (validators.length != request.getMetricData().size()) {
                errorText = "Got " + request.getMetricData().size() + " data elements, but had only " + validators.length + " validators.";
                return false;
            }
            for (int i = 0; i < request.getMetricData().size(); i++) {
                try {
                    validators[i].validate(request.getMetricData().get(i));
                } catch (Exception e) {
                    errorText = e.getMessage();
                    return false;
                }
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            if (errorText != null) {
                description.appendText(errorText);
            }
        }
    }

}
