package com.kixeye.chassis.chassis.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.codahale.metrics.Metric;
import com.google.common.base.Preconditions;

/**
 * Filters metrics that should be reported on
 *
 * @author dturner@kixeye.com
 */
public class MetricFilter implements com.codahale.metrics.MetricFilter {

    public static final MetricFilter FILTER_NONE = new MetricFilter("*");

    private HashMap<Pattern, MetricDescriptor> patterns = new HashMap<>();
    private String stringValue;

    @Autowired
    public MetricFilter(String filter) {
        Preconditions.checkNotNull(filter);
        this.stringValue = filter;
        parse(filter);
    }

    private void parse(String filter) {
        filter = filter.trim();
        if ("".equals(filter) || "*".equals(filter)) {
           stringValue = "*";
           return;
        }
        String[] metricDefinitions = filter.split(",");
        if (metricDefinitions.length == 0) {
            return;
        }
        for (String metricDef : metricDefinitions) {
            MetricDescriptor descriptor = MetricDescriptor.parse(metricDef);
            patterns.put(descriptor.getPattern(), descriptor);
        }
    }

    @Override
    public boolean matches(String metricName, Metric metric) {
        if (patterns.isEmpty()) {
            return true;
        }
        for (Pattern pattern : patterns.keySet()) {
            if (pattern.matcher(metricName).matches()) {
                return true;
            }
        }
        return false;
    }

    public MetricDescriptor getMatchingMetricDescriptor(String metricName, Stat stat) {
        for (Pattern pattern : patterns.keySet()) {
            if (pattern.matcher(metricName).matches()) {
                MetricDescriptor metricDescriptor = patterns.get(pattern);
                if (metricDescriptor.containsStat(stat)) {
                    return metricDescriptor;
                }
            }
        }
        return null;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static enum Stat {
        MIN("min"),
        COUNT("count"),
        MAX("max"),
        MEAN("mean"),
        RATE_1_MINUTE("1m"),
        RATE_5_MINUTE("5m"),
        RATE_15_MINUTE("15m"),
        PERCENTILE_75("75p"),
        PERCENTILE_95("95p"),
        PERCENTILE_98("98p"),
        PERCENTILE_99("99p"),
        PERCENTILE_999("999p"),
        STDDEV("stddev"),
        ALL("ALL");
        private static Map<String, Stat> statsByStringValue = new HashMap<>();
        private final String stringValue;

        private Stat(String stringValue) {
            this.stringValue = stringValue;
        }

        public static Stat fromStringValue(String stringValue) {
            if (statsByStringValue.isEmpty()) {
                for (Stat stat : Stat.values()) {
                    statsByStringValue.put(stat.getStringValue(), stat);
                }
            }
            Stat stat = statsByStringValue.get(stringValue);
            if (stat == null) {
                throw new IllegalArgumentException("No enum constant with string value " + stringValue);
            }
            return stat;
        }

        public String getStringValue() {
            return stringValue;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricFilter that = (MetricFilter) o;

        if (!stringValue.equals(that.stringValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return stringValue.hashCode();
    }
}
