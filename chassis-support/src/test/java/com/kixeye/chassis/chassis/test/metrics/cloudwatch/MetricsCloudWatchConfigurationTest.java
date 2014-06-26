package com.kixeye.chassis.chassis.test.metrics.cloudwatch;

import com.codahale.metrics.MetricRegistry;
import com.kixeye.chassis.chassis.metrics.MetricFilter;
import com.kixeye.chassis.chassis.metrics.aws.MetricsCloudWatchConfiguration;
import com.kixeye.chassis.chassis.metrics.aws.MetricsCloudWatchReporter;
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
@ContextConfiguration(classes = {MetricsCloudWatchConfigurationTest.class, MetricsCloudWatchConfiguration.class})
public class MetricsCloudWatchConfigurationTest {

    @Autowired
    private MetricsCloudWatchConfiguration metricsCloudWatchConfiguration;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeClass
    public static void beforeClass() {
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.APP_NAME_PROPERTY_NAME), "test");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.APP_VERSION_PROPERTY_NAME), "test");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.APP_ENVIRONMENT_PROPERTY_NAME), "test");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.METRICS_AWS_FILTER_PROPERTY_NAME), "foo=bar");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.METRICS_AWS_PUBLISH_INTERVAL_PROPERTY_NAME), "1");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.METRICS_AWS_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME), "MINUTES");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.METRICS_AWS_REGION_PROPERTY_NAME), "default");
        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchConfiguration.METRICS_AWS_ENABLED_PROPERTY_NAME), "true");
    }

    private static String removePlaceholder(String placeholder) {
        return placeholder.replaceFirst("\\$", "").replaceFirst("\\{", "").replaceFirst("}", "");
    }

    @Test
    public void testStartContextWithReporterEnabled() {
        Assert.assertTrue(metricsCloudWatchConfiguration.isEnabled());
        Assert.assertNotNull(metricsCloudWatchConfiguration.getReporter());
    }

    @Test
    public void testStopStartedReporter() {
        Assert.assertTrue(metricsCloudWatchConfiguration.isEnabled());
        Assert.assertNotNull(metricsCloudWatchConfiguration.getReporter());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchConfiguration.METRICS_AWS_ENABLED_PROPERTY_NAME), "false");

        Assert.assertFalse(metricsCloudWatchConfiguration.isEnabled());
        Assert.assertNull(metricsCloudWatchConfiguration.getReporter());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchConfiguration.METRICS_AWS_ENABLED_PROPERTY_NAME), "true");
    }

    @Test
    public void testRestartReporter() {
        Assert.assertTrue(metricsCloudWatchConfiguration.isEnabled());
        Assert.assertNotNull(metricsCloudWatchConfiguration.getReporter());
        MetricsCloudWatchReporter originalReporter = metricsCloudWatchConfiguration.getReporter();

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchConfiguration.METRICS_AWS_ENABLED_PROPERTY_NAME), "false");

        Assert.assertFalse(metricsCloudWatchConfiguration.isEnabled());
        Assert.assertNull(metricsCloudWatchConfiguration.getReporter());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchConfiguration.METRICS_AWS_ENABLED_PROPERTY_NAME), "true");

        Assert.assertTrue(metricsCloudWatchConfiguration.isEnabled());
        Assert.assertNotNull(metricsCloudWatchConfiguration.getReporter());
        Assert.assertNotSame(originalReporter, metricsCloudWatchConfiguration.getReporter());
    }

    @Test
    public void testUpdateFilter() {
        MetricFilter filter = metricsCloudWatchConfiguration.getReporter().getFilter();
        Assert.assertEquals("foo=bar", filter.getStringValue());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.METRICS_AWS_FILTER_PROPERTY_NAME), "foo=baz");

        Assert.assertNotEquals(filter, metricsCloudWatchConfiguration.getReporter().getFilter());
        Assert.assertEquals("foo=baz", metricsCloudWatchConfiguration.getReporter().getFilter().getStringValue());
    }

    @Test
    public void testUpdatePublishIntervalUnit() {
        TimeUnit originalIntervalUnit = metricsCloudWatchConfiguration.getReporter().getPublishIntervalUnit();
        Assert.assertEquals(originalIntervalUnit, TimeUnit.MINUTES);

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.METRICS_AWS_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME), TimeUnit.SECONDS + "");

        Assert.assertEquals(TimeUnit.SECONDS, metricsCloudWatchConfiguration.getReporter().getPublishIntervalUnit());

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.METRICS_AWS_PUBLISH_INTERVAL_UNIT_PROPERTY_NAME), TimeUnit.MINUTES + "");
    }

    @Test
    public void testUpdatePublishInterval() {
        int originalInterval = metricsCloudWatchConfiguration.getReporter().getPublishInterval();
        int newInterval = originalInterval + 1;

        ConfigurationManager.getConfigInstance().setProperty(removePlaceholder(MetricsCloudWatchReporter.METRICS_AWS_PUBLISH_INTERVAL_PROPERTY_NAME), newInterval + "");

        Assert.assertEquals(newInterval, metricsCloudWatchConfiguration.getReporter().getPublishInterval());
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
