package com.kixeye.chassis.chassis.metrics;

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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.kixeye.chassis.chassis.metrics.MetricFilter.Stat;

/**
 * Describes a metric as it will be pushed to external reporting systems.
 *
 * @author dturner@kixeye.com
 */
public class MetricDescriptor {
    private Pattern pattern;
    private String alias;
    private Set<Stat> stats = new HashSet<>();

    private MetricDescriptor(String alias, Pattern pattern) {
        this.alias = alias;
        this.pattern = pattern;
    }

    public static MetricDescriptor parse(String descriptor) {
        String[] tokensByEquals = descriptor.split("=");

        if (tokensByEquals.length != 2) {
            throw new IllegalArgumentException("Invalid filter format. Expected format is {alias}={metric pattern}[:{stats}].");
        }

        String alias = tokensByEquals[0];
        String details = tokensByEquals[1];

        String[] tokensByColon = details.split(":");

        if (tokensByColon.length == 0) {
            throw new IllegalArgumentException("Invalid filter format. Expected format is {alias}={metric pattern}[:{stats}].");
        }
        String pattern = tokensByColon[0];

        MetricDescriptor metricDescriptor = new MetricDescriptor(alias, Pattern.compile(pattern));
        if (tokensByColon.length == 1) {
            //empty stats, mean all stats are supported.
            metricDescriptor.addStat(Stat.ALL);
            return metricDescriptor;
        }
        for (int i = 1; i < tokensByColon.length; i++) {
            metricDescriptor.addStat(Stat.fromStringValue(tokensByColon[i]));
        }
        return metricDescriptor;
    }

    private void addStat(Stat stat) {
        stats.add(stat);
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Set<Stat> getStats() {
        return stats;
    }

    public boolean containsStat(Stat stat) {
        return stats.contains(Stat.ALL) || stats.contains(stat);
    }

    public String getAlias() {
        return alias;
    }
}
