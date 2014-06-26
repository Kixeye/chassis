package com.kixeye.chassis.chassis.test.metrics.graphite;

import com.codahale.metrics.MetricRegistry;
import com.kixeye.chassis.chassis.metrics.MetricFilter;
import com.kixeye.chassis.chassis.metrics.aws.MetricsCloudWatchReporter;
import com.kixeye.chassis.chassis.metrics.codahale.MetricsGraphiteConfiguration;
import com.kixeye.chassis.chassis.metrics.codahale.MetricsGraphiteReporterLoader;
import com.netflix.config.ConfigurationManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author dturner@kixeye.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MetricsGraphiteConfigurationTest.class, MetricsGraphiteConfiguration.class})
public class MetricsGraphiteConfigurationTest {

    @Autowired
    private MetricsGraphiteConfiguration metricsGraphiteConfiguration;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeClass
    public static void beforeClass() {
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.APP_NAME_PROPERTY_NAME), "test");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.APP_VERSION_PROPERTY_NAME), "test");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.APP_ENVIRONMENT_PROPERTY_NAME), "test");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_SERVER_PROPERTY_NAME), "localhost");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PORT_PROPERTY_NAME), "80");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_FILTER_PROPERTY_NAME), "foo=bar");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PUBLISH_INTERVAL_PROPERTY_NAME), "1");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME), "MINUTES");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteConfiguration.METRICS_GRAPHITE_ENABLED_PROPERTY_NAME), "true");
    }

    private static String removePlaceholder(String placeholder) {
        return placeholder.replaceFirst("\\$", "").replaceFirst("\\{", "").replaceFirst("}", "");
    }

    @Test
    public void testStartContextWithReporterEnabled() {
        Assert.assertTrue(metricsGraphiteConfiguration.isEnabled());
        Assert.assertNotNull(metricsGraphiteConfiguration.getReporterLoader());
    }

    @Test
    public void testStopStartedReporter() {
        Assert.assertTrue(metricsGraphiteConfiguration.isEnabled());
        Assert.assertNotNull(metricsGraphiteConfiguration.getReporterLoader());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteConfiguration.METRICS_GRAPHITE_ENABLED_PROPERTY_NAME), "false");

        Assert.assertFalse(metricsGraphiteConfiguration.isEnabled());
        Assert.assertNull(metricsGraphiteConfiguration.getReporterLoader());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteConfiguration.METRICS_GRAPHITE_ENABLED_PROPERTY_NAME), "true");
    }

    @Test
    public void testRestartReporter() {
        Assert.assertTrue(metricsGraphiteConfiguration.isEnabled());
        Assert.assertNotNull(metricsGraphiteConfiguration.getReporterLoader());
        MetricsGraphiteReporterLoader originalLoader = metricsGraphiteConfiguration.getReporterLoader();

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteConfiguration.METRICS_GRAPHITE_ENABLED_PROPERTY_NAME), "false");

        Assert.assertFalse(metricsGraphiteConfiguration.isEnabled());
        Assert.assertNull(metricsGraphiteConfiguration.getReporterLoader());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteConfiguration.METRICS_GRAPHITE_ENABLED_PROPERTY_NAME), "true");

        Assert.assertTrue(metricsGraphiteConfiguration.isEnabled());
        Assert.assertNotNull(metricsGraphiteConfiguration.getReporterLoader());
        Assert.assertNotSame(originalLoader, metricsGraphiteConfiguration.getReporterLoader());
    }

    @Test
    public void testUpdateFilter() {
        MetricFilter filter = metricsGraphiteConfiguration.getReporterLoader().getFilter();
        Assert.assertEquals("foo=bar", filter.getStringValue());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_FILTER_PROPERTY_NAME), "foo=baz");

        Assert.assertNotEquals(filter, metricsGraphiteConfiguration.getReporterLoader().getFilter());
        Assert.assertEquals("foo=baz", metricsGraphiteConfiguration.getReporterLoader().getFilter().getStringValue());
    }

    @Test
    public void testUpdatePublishIntervalUnit() {
        TimeUnit originalIntervalUnit = metricsGraphiteConfiguration.getReporterLoader().getPublishIntervalUnit();
        Assert.assertEquals(originalIntervalUnit, TimeUnit.MINUTES);

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME), TimeUnit.SECONDS + "");

        Assert.assertEquals(TimeUnit.SECONDS, metricsGraphiteConfiguration.getReporterLoader().getPublishIntervalUnit());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME), TimeUnit.MINUTES + "");
    }

    @Test
    public void testUpdatePublishInterval() {
        int originalInterval = metricsGraphiteConfiguration.getReporterLoader().getPublishInterval();
        int newInterval = originalInterval + 1;

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PUBLISH_INTERVAL_PROPERTY_NAME), newInterval + "");

        Assert.assertEquals(newInterval, metricsGraphiteConfiguration.getReporterLoader().getPublishInterval());
    }


    @Test
    public void testUpdateGraphiteServerPort() {
        int originalPort = metricsGraphiteConfiguration.getReporterLoader().getGraphitePort();
        int newPort = originalPort + 1;

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_PORT_PROPERTY_NAME), newPort + "");

        Assert.assertEquals(newPort, metricsGraphiteConfiguration.getReporterLoader().getGraphitePort());
    }

    @Test
    public void testUpdateGraphiteServerHost() {
        String originalHost = metricsGraphiteConfiguration.getReporterLoader().getGraphiteServer();
        Assert.assertNotEquals("127.0.0.1", originalHost);
        String newHost = "127.0.0.1";

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsGraphiteReporterLoader.METRICS_GRAPHITE_SERVER_PROPERTY_NAME), newHost);

        Assert.assertEquals(newHost, metricsGraphiteConfiguration.getReporterLoader().getGraphiteServer());
    }

    @Bean
    public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertyPlaceholderConfigurer() {
            @Override
            protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
                return ConfigurationManager.getConfigInstance().getString(placeholder);
            }
        };
    }

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }
}
