package com.yeshwanth.cloudmonitor.dto;

import java.util.List;

/**
 * A metric series for one instance, plus the metadata the chart needs to label itself.
 *
 * @param instanceId    EC2 instance the points belong to
 * @param metric        CloudWatch metric name, e.g. {@code CPUUtilization}
 * @param unit          unit of {@link #points()} values, e.g. {@code percent}
 * @param source        whether the values came from AWS or were generated in demo mode
 * @param periodSeconds CloudWatch aggregation period used for the query
 * @param points        datapoints ordered oldest to newest; empty when AWS has no data yet
 */
public record MetricSeries(
        String instanceId,
        String metric,
        String unit,
        Source source,
        int periodSeconds,
        List<MetricPoint> points) {

    public enum Source { AWS, DEMO }

    public static MetricSeries empty(String instanceId, String metric, String unit, Source source, int periodSeconds) {
        return new MetricSeries(instanceId, metric, unit, source, periodSeconds, List.of());
    }
}
