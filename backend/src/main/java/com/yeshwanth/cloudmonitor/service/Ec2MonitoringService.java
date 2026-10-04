package com.yeshwanth.cloudmonitor.service;

import com.yeshwanth.cloudmonitor.config.AwsProperties;
import com.yeshwanth.cloudmonitor.dto.InstanceSummary;
import com.yeshwanth.cloudmonitor.dto.MetricPoint;
import com.yeshwanth.cloudmonitor.dto.MetricSeries;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the EC2 inventory and CloudWatch CPU history for the account in
 * {@link AwsProperties#region()}.
 */
@Service
public class Ec2MonitoringService {

    private static final String CPU_METRIC = "CPUUtilization";
    private static final String CPU_UNIT = "percent";
    private static final String NAMESPACE = "AWS/EC2";
    private static final String QUERY_ID = "cpu";

    private final Ec2Client ec2Client;
    private final CloudWatchClient cloudWatchClient;
    private final AwsProperties properties;
    private final DemoData demoData;

    public Ec2MonitoringService(Ec2Client ec2Client,
                               CloudWatchClient cloudWatchClient,
                               AwsProperties properties,
                               DemoData demoData) {
        this.ec2Client = ec2Client;
        this.cloudWatchClient = cloudWatchClient;
        this.properties = properties;
        this.demoData = demoData;
    }

    public List<InstanceSummary> instances() {
        if (properties.demoMode()) {
            return demoData.instances();
        }
        return ec2Client.describeInstances(DescribeInstancesRequest.builder().build())
                .reservations()
                .stream()
                .flatMap(reservation -> reservation.instances().stream())
                .map(InstanceSummary::from)
                .toList();
    }

    /**
     * @param instanceId EC2 instance id to read
     * @param hours      look-back window in hours; already range-checked by the controller
     */
    public MetricSeries cpuHistory(String instanceId, int hours) {
        int periodSeconds = periodFor(hours);
        if (properties.demoMode()) {
            return demoData.cpuHistory(instanceId, hours, periodSeconds);
        }

        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        GetMetricDataResponse response = cloudWatchClient.getMetricData(GetMetricDataRequest.builder()
                .startTime(start)
                .endTime(end)
                .metricDataQueries(MetricDataQuery.builder()
                        .id(QUERY_ID)
                        .returnData(true)
                        .metricStat(MetricStat.builder()
                                .metric(Metric.builder()
                                        .namespace(NAMESPACE)
                                        .metricName(CPU_METRIC)
                                        .dimensions(Dimension.builder()
                                                .name("InstanceId")
                                                .value(instanceId)
                                                .build())
                                        .build())
                                .period(periodSeconds)
                                .stat(Statistic.AVERAGE)
                                .build())
                        .build())
                .build());

        return toSeries(instanceId, periodSeconds, response);
    }

    /** Coarser resolution for longer windows keeps every CloudWatch response inside its limits. */
    static int periodFor(int hours) {
        if (hours <= 1) {
            return 60;
        }
        if (hours <= 12) {
            return 300;
        }
        return 900;
    }

    private MetricSeries toSeries(String instanceId, int periodSeconds, GetMetricDataResponse response) {
        List<MetricDataResult> results = response.metricDataResults();
        if (results.isEmpty() || results.get(0).timestamps().isEmpty()) {
            return MetricSeries.empty(instanceId, CPU_METRIC, CPU_UNIT, MetricSeries.Source.AWS, periodSeconds);
        }
        MetricDataResult result = results.get(0);
        List<Instant> timestamps = result.timestamps();
        List<Double> values = result.values();
        int size = Math.min(timestamps.size(), values.size());
        List<MetricPoint> points = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            points.add(new MetricPoint(timestamps.get(i), values.get(i)));
        }
        return new MetricSeries(instanceId, CPU_METRIC, CPU_UNIT, MetricSeries.Source.AWS,
                periodSeconds, List.copyOf(points));
    }
}
