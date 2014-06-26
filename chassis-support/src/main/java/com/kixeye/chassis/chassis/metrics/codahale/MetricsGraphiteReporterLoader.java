package com.kixeye.chassis.chassis.metrics.codahale;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.common.base.Strings;
import com.kixeye.chassis.chassis.metrics.MetricFilter;
import com.kixeye.chassis.chassis.util.NetworkingUtils;

/**
 * A wrapper around GraphiteReporter used as a Spring prototype bean.
 *
 * @author dturner@kixeye.com
 */
@Component(MetricsGraphiteReporterLoader.BEAN_NAME)
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MetricsGraphiteReporterLoader {
    public static final String BEAN_NAME = "MetricsGraphiteReporterLoader";
    public static final String APP_ENVIRONMENT_PROPERTY_NAME = "${app.environment}";
    public static final String APP_NAME_PROPERTY_NAME = "${app.name}";
    public static final String APP_VERSION_PROPERTY_NAME = "${app.version}";
    public static final String METRICS_GRAPHITE_SERVER_PROPERTY_NAME = "${metrics.graphite.server}";
    public static final String METRICS_GRAPHITE_PORT_PROPERTY_NAME = "${metrics.graphite.port}";
    public static final String METRICS_GRAPHITE_FILTER_PROPERTY_NAME = "${metrics.graphite.filter}";
    public static final String METRICS_GRAPHITE_PUBLISH_INTERVAL_PROPERTY_NAME = "${metrics.graphite.publish-interval}";
    public static final String METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME = "${metrics.graphite.publish-interval-unit}";
    private static final Logger logger = LoggerFactory.getLogger(MetricsGraphiteReporterLoader.class);
    private String graphiteServer;
    private int graphitePort;
    private Graphite graphite;
    private int publishInterval;
    private TimeUnit publishIntervalUnit;
    private GraphiteReporter graphiteReporter;
    private MetricFilter filter;

    @Autowired
    public MetricsGraphiteReporterLoader(@Value(METRICS_GRAPHITE_SERVER_PROPERTY_NAME) String server,
                                         @Value(METRICS_GRAPHITE_PORT_PROPERTY_NAME) int port,
                                         @Value(METRICS_GRAPHITE_FILTER_PROPERTY_NAME) String metricFilter,
                                         @Value(METRICS_GRAPHITE_PUBLISH_INTERVAL_PROPERTY_NAME) int publishInterval,
                                         @Value(METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME) TimeUnit publishIntervalUnit,
                                         @Value(APP_ENVIRONMENT_PROPERTY_NAME) String environment,
                                         @Value(APP_NAME_PROPERTY_NAME) String serviceName,
                                         @Value(APP_VERSION_PROPERTY_NAME) String version,
                                         MetricRegistry metricRegistry) {

        logger.debug("Creating instance of {} with server:{}, port:{}, filter:{}, service:{}, environment:{}, version:{}",
                getClass().getSimpleName(), server, port, metricFilter, serviceName, environment, version);

        this.publishInterval = publishInterval;
        this.publishIntervalUnit = publishIntervalUnit;
        this.graphiteServer = server;
        this.graphitePort = port;

        createGraphite();
        createReporter(metricFilter, environment, serviceName, version, metricRegistry);
    }

    public void start() {
        this.graphiteReporter.start(publishInterval, publishIntervalUnit);
    }

    public MetricFilter getFilter() {
        return filter;
    }
    
    public GraphiteReporter getReporter() {
    	return graphiteReporter;
    }

    public String getGraphiteServer() {
        return graphiteServer;
    }

    public int getGraphitePort() {
        return graphitePort;
    }

    public int getPublishInterval() {
        return publishInterval;
    }

    public TimeUnit getPublishIntervalUnit() {
        return publishIntervalUnit;
    }

    @PreDestroy
    public void stop() {
        this.graphiteReporter.stop();
        try {
            this.graphite.close();
        } catch (IOException e) {
            logger.warn("Failed to close graphite",e);
        }
    }

    private void createGraphite() {
        this.graphite = new Graphite(new InetSocketAddress(graphiteServer, graphitePort));
    }

    private void createReporter(String metricFilter, String environment, String serviceName, String version, MetricRegistry metricRegistry) {
        // optionally filter metrics to send to graphite server
        MetricFilter filter = MetricFilter.FILTER_NONE;

        if (!(Strings.isNullOrEmpty(metricFilter) || "*".equals(metricFilter))) {
            filter = new MetricFilter(metricFilter);
        }

        this.filter = filter;

        this.graphiteReporter = GraphiteReporter
                .forRegistry(metricRegistry)
                .prefixedWith(getPreFix(environment, serviceName, version))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(filter)
                .build(graphite);

    }

    private String getPreFix(String environment, String serviceName, String version) {
        return String.format("%1s.%2s.%3s.%4s",
                environment.replace('.', '_'),
                serviceName.replace('.', '_'),
                version.replace('.', '_'),
                NetworkingUtils.getApplicationIdentifier());
    }
}


