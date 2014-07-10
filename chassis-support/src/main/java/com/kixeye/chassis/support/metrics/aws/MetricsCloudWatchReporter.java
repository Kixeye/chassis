package com.kixeye.chassis.support.metrics.aws;

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

import java.util.Date;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.kixeye.chassis.support.metrics.MetricDescriptor;
import com.kixeye.chassis.support.metrics.MetricFilter;
import com.kixeye.chassis.support.metrics.MetricFilter.Stat;

/**
 * A Coda Hale Metrics Reporter that send metrics to Amazon CloudWatch.
 *
 * @author dturner@kixeye.com
 */
@Component(MetricsCloudWatchReporter.BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MetricsCloudWatchReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCloudWatchReporter.class);

    public static final String APP_NAME_PROPERTY_NAME = "${app.name}";
    public static final String APP_VERSION_PROPERTY_NAME = "${app.version}";
    public static final String APP_ENVIRONMENT_PROPERTY_NAME = "${app.environment}";
    public static final String METRICS_AWS_FILTER_PROPERTY_NAME = "${metrics.aws.filter}";
    public static final String METRICS_AWS_PUBLISH_INTERVAL_PROPERTY_NAME = "${metrics.aws.publish-interval}";
    public static final String METRICS_AWS_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME = "${metrics.aws.publish-interval-unit}";
    public static final String AWS_ACCESS_ID_PROPERTY_NAME = "${aws.accessId:}";
    public static final String AWS_SECRET_KEY_PROPERTY_NAME = "${aws.secretKey:}";
    public static final String METRICS_AWS_REGION_PROPERTY_NAME = "${metrics.aws.region}";
    public static final String BEAN_NAME = "MetricsCloudWatchReporter";

    public static final int MAX_CLOUDWATCH_DATUM_PER_REQUEST = 20;

    private AmazonCloudWatch cloudWatch;
    private String appName;
    private String appEnvironment;
    private MetricFilter filter;
    private int publishInterval;
    private TimeUnit publishIntervalUnit;

    @Autowired
    public MetricsCloudWatchReporter(
            @Value(APP_NAME_PROPERTY_NAME) String appName,
            @Value(APP_VERSION_PROPERTY_NAME) String appVersion,
            @Value(APP_ENVIRONMENT_PROPERTY_NAME) String appEnvironment,
            @Value(METRICS_AWS_FILTER_PROPERTY_NAME) String metricFilter,
            @Value(METRICS_AWS_PUBLISH_INTERVAL_PROPERTY_NAME) int publishInterval,
            @Value(METRICS_AWS_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME) TimeUnit publishIntervalUnit,
            MetricRegistry metricRegistry,
            CloudWatchFactory cloudWatchFactory) {
        super(metricRegistry, "cloudwatch-reporter", new MetricFilter(metricFilter), TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.filter = new MetricFilter(metricFilter);
        this.appName = appName;
        this.appEnvironment = appEnvironment;
        this.publishInterval = publishInterval;
        this.publishIntervalUnit = publishIntervalUnit;
        this.cloudWatch = cloudWatchFactory.getCloudWatchClient();

        logger.debug("Created instance of {} with properties: app:{}, version:{}, environment:{}, filter:{}, publishInterval:{}, publishIntervalUnit:{}",
                getClass().getSimpleName(), appName, appVersion, appEnvironment, metricFilter, publishInterval, publishIntervalUnit);
    }

    @Override
    @PreDestroy
    public void stop() {
        super.stop();
        cloudWatch.shutdown();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void report(
            SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {

        logger.info("Starting metrics publishing to AWS CloudWatch.");

        LinkedList<PutMetricDataRequest> requests = new LinkedList<>();

        addMetricData(gauges, counters, histograms, meters, timers, requests, new Date());

        if (requests.isEmpty()) {
            logger.debug("No metric data to send to AWS.");
            return;
        }

        for (PutMetricDataRequest request : requests) {
            try {
                for (MetricDatum datum : request.getMetricData()) {
                    logger.debug("Sending metric " + datum);
                }
                cloudWatch.putMetricData(request);
            } catch (Exception e) {
                logger.error("Failed to log metrics to CloudWatch discarding metrics for this attempt...",e);
                return;
            }
        }
        logger.info("Finished metrics publishing to AWS CloudWatch.");
    }

    public MetricFilter getFilter() {
        return filter;
    }

    public int getPublishInterval() {
        return publishInterval;
    }

    public TimeUnit getPublishIntervalUnit() {
        return publishIntervalUnit;
    }

    private PutMetricDataRequest createRequest() {
        return new PutMetricDataRequest().withNamespace(appName);
    }

    @SuppressWarnings("rawtypes")
    private void addMetricData(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        addGauges(gauges, requests, timestamp);
        addCounters(counters, requests, timestamp);
        addHistograms(histograms, requests, timestamp);
        addMeters(meters, requests, timestamp);
        addTimers(timers, requests, timestamp);
    }

    private void addCounters(SortedMap<String, Counter> counters, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        logger.debug("Adding Counters...");
        for (String name : counters.keySet()) {
            Counter counter = counters.get(name);
            addDatum(filter.getMatchingMetricDescriptor(name, Stat.ALL).getAlias(), counter.getCount(), requests, timestamp);
        }
    }

    private void addHistograms(SortedMap<String, Histogram> histograms, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        logger.debug("Adding Histograms...");
        for (String name : histograms.keySet()) {
            Histogram histogram = histograms.get(name);
            Snapshot snapshot = histogram.getSnapshot();
            checkAndAddDatum(MetricFilter.Stat.COUNT, name, histogram.getCount(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.MIN, name, snapshot.getMin(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.MAX, name, snapshot.getMax(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.MEAN, name, snapshot.getMean(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.STDDEV, name, snapshot.getStdDev(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.PERCENTILE_75, name, snapshot.get75thPercentile(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.PERCENTILE_95, name, snapshot.get95thPercentile(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.PERCENTILE_98, name, snapshot.get98thPercentile(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.PERCENTILE_99, name, snapshot.get99thPercentile(), requests, timestamp);
            checkAndAddDatum(MetricFilter.Stat.PERCENTILE_999, name, snapshot.get999thPercentile(), requests, timestamp);
        }
    }

    private void addMeters(SortedMap<String, Meter> meters, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        logger.debug("Adding Meters...");
        for (String name : meters.keySet()) {
            addMetered(name, meters.get(name), requests, timestamp);
        }
    }

    private void addMetered(String name, Metered metered, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        checkAndAddDatum(MetricFilter.Stat.RATE_1_MINUTE, name, metered.getOneMinuteRate(), requests, timestamp);
        checkAndAddDatum(MetricFilter.Stat.RATE_5_MINUTE, name, metered.getFiveMinuteRate(), requests, timestamp);
        checkAndAddDatum(MetricFilter.Stat.RATE_15_MINUTE, name, metered.getFifteenMinuteRate(), requests, timestamp);
        checkAndAddDatum(MetricFilter.Stat.MEAN, name, metered.getMeanRate(), requests, timestamp);
    }

    private void addTimers(SortedMap<String, Timer> timers, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        logger.debug("Adding Timers...");
        for (String name : timers.keySet()) {
            Timer timer = timers.get(name);
            checkAndAddDatum(MetricFilter.Stat.COUNT, name, timer.getCount(), requests, timestamp);
            addMetered(name, timer, requests, timestamp);
        }
    }

    //check the filter to see if the given stat of the given metric should be added and add if so.
    private void checkAndAddDatum(Stat stat, String name, double value, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        MetricDescriptor descriptor = filter.getMatchingMetricDescriptor(name, stat);
        if (descriptor != null) {
            addDatum(descriptor.getAlias() + "." + stat.getStringValue(), value, requests, timestamp);
        }
    }

    @SuppressWarnings("rawtypes")
    private void addGauges(SortedMap<String, Gauge> gauges, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        logger.debug("Adding Gauges...");
        for (String name : gauges.keySet()) {
            Gauge<?> gauge = gauges.get(name);
            if (!(gauge.getValue() instanceof Number)) {
                logger.warn("Encountered Gauge with non-numeric value. Gauge:{}, Value:{}", name, gauge.getValue());
                continue;
            }
            Double value = ((Number) gauge.getValue()).doubleValue();
            addDatum(filter.getMatchingMetricDescriptor(name, Stat.ALL).getAlias(), value, requests, timestamp);
        }
    }

    private void addDatum(String name, double value, LinkedList<PutMetricDataRequest> requests, Date timestamp) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding Datum {} with value {} at {}", name, value, timestamp);
        }
        if (requests.isEmpty() || requests.getLast().getMetricData().size() == MAX_CLOUDWATCH_DATUM_PER_REQUEST) {
            requests.add(createRequest());
        }
        PutMetricDataRequest request = requests.getLast();
        MetricDatum datum = new MetricDatum().withTimestamp(timestamp).withValue(value).withMetricName(name).withUnit(StandardUnit.None).withDimensions(createDimensions());
        request.withMetricData(datum);
    }

    private Dimension[] createDimensions() {
        return new Dimension[]{new Dimension().withName("environment").withValue(appEnvironment)};
    }

    public void start() {
        start(publishInterval, publishIntervalUnit);
    }
}
