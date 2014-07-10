package com.kixeye.chassis.support.test.metrics;

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

import com.codahale.metrics.Metric;
import com.kixeye.chassis.support.metrics.MetricFilter;
import com.kixeye.chassis.support.metrics.MetricFilter.Stat;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for MetricFilter
 *
 * @author dturner@kixeye.com
 */
public class MetricFilterTest {
    @Test
    public void matchesAsterix() {
        MetricFilter filter = new MetricFilter("*");
        Assert.assertTrue(filter.matches("mymetric", new Metric() {
        }));
    }

    @Test
    public void matchesEmpty() {
        MetricFilter filter = new MetricFilter("");
        Assert.assertTrue(filter.matches("mymetric", new Metric() {
        }));
    }

    @Test
    public void exactMatchSingleMetric() {
        MetricFilter filter = new MetricFilter("MyMetric=com.kixeye.MyMetric");
        Assert.assertTrue(filter.matches("com.kixeye.MyMetric", new Metric() {
        }));
    }

    @Test
    public void patternMatchSingleMetric() {
        MetricFilter filter = new MetricFilter("MyMetric=com.[a-zA-Z0-9]*.MyMetric");
        Assert.assertTrue(filter.matches("com.kixeye.MyMetric", new Metric() {
        }));
    }

    @Test
    public void exactMatchMultipleMetrics() {
        MetricFilter filter = new MetricFilter("MyMetric1=com.kixeye.MyMetric1,MyMetric2=com.kixeye.MyMetric2");
        Assert.assertTrue(filter.matches("com.kixeye.MyMetric1", new Metric() {
        }));
        Assert.assertTrue(filter.matches("com.kixeye.MyMetric2", new Metric() {
        }));
        Assert.assertFalse(filter.matches("com.kixeye.MyMetric3", new Metric() {
        }));
    }

    @Test
    public void exactMatchMultipleMetricsWithStatsFilter() {
        MetricFilter filter = new MetricFilter("MyMetric1=com.kixeye.MyMetric1:5m,MyMetric2=com.kixeye.MyMetric2:5m");
        Assert.assertTrue(filter.matches("com.kixeye.MyMetric1", new Metric() {
        }));
        Assert.assertTrue(filter.matches("com.kixeye.MyMetric2", new Metric() {
        }));
        Assert.assertFalse(filter.matches("com.kixeye.MyMetric3", new Metric() {
        }));

        Assert.assertNotNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric1", Stat.RATE_5_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric1", Stat.RATE_15_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric1", Stat.ALL));

        Assert.assertNotNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric2", Stat.RATE_5_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric2", Stat.RATE_15_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric2", Stat.ALL));

        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric3", Stat.RATE_5_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric3", Stat.RATE_15_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric3", Stat.ALL));
    }

    @Test
    public void patternMatchMultipleMetricsWithStatsFilter() {
        MetricFilter filter = new MetricFilter("MyMetric1=com.[a-zA-Z0-9]*.MyMetric1:5m,MyMetric2=com.[a-zA-Z0-9]*.MyMetric2:5m");
        Assert.assertTrue(filter.matches("com.kixeye.MyMetric1", new Metric() {
        }));
        Assert.assertTrue(filter.matches("com.kixeye.MyMetric2", new Metric() {
        }));
        Assert.assertFalse(filter.matches("com.kixeye.MyMetric3", new Metric() {
        }));

        Assert.assertNotNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric1", Stat.RATE_5_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric1", Stat.RATE_15_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric1", Stat.ALL));

        Assert.assertNotNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric2", Stat.RATE_5_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric2", Stat.RATE_15_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric2", Stat.ALL));

        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric3", Stat.RATE_5_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric3", Stat.RATE_15_MINUTE));
        Assert.assertNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric3", Stat.ALL));
    }

    @Test
    public void multiplePatternMatches() {
        MetricFilter filter = new MetricFilter("MyMetric1=com.kixeye.MyMetric[0-9]:5m,MyMetric2=com.[a-zA-Z0-9]*.MyMetric[0-9]:15m");

        Assert.assertTrue(filter.matches("com.kixeye.MyMetric1", new Metric() {
        }));

        Assert.assertTrue(filter.matches("com.foo.MyMetric1", new Metric() {
        }));

        Assert.assertNotNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric1", Stat.RATE_5_MINUTE));
        Assert.assertNotNull(filter.getMatchingMetricDescriptor("com.kixeye.MyMetric1", Stat.RATE_15_MINUTE));

        Assert.assertNull(filter.getMatchingMetricDescriptor("com.foo.MyMetric1", Stat.RATE_5_MINUTE));
        Assert.assertNotNull(filter.getMatchingMetricDescriptor("com.foo.MyMetric1", Stat.RATE_15_MINUTE));
    }
}
