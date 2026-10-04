package com.yeshwanth.cloudmonitor.dto;

import java.time.Instant;

/** A single CloudWatch datapoint. */
public record MetricPoint(Instant timestamp, double value) {
}
