package com.yeshwanth.cloudmonitor.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Ec2MonitoringService {
    private final Ec2Client ec2;
    private final CloudWatchClient cloudWatch;

    public Ec2MonitoringService(Ec2Client ec2, CloudWatchClient cloudWatch) {
        this.ec2 = ec2;
        this.cloudWatch = cloudWatch;
    }

    public List<Map<String, Object>> instances() {
        var response = ec2.describeInstances(DescribeInstancesRequest.builder().build());
        List<Map<String, Object>> result = new ArrayList<>();
        response.reservations().forEach(r -> r.instances().forEach(i -> result.add(instanceMap(i))));
        return result;
    }

    public Map<String, Object> cpu(String instanceId, int hours) {
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        Metric metric = Metric.builder().namespace("AWS/EC2").metricName("CPUUtilization")
                .dimensions(Dimension.builder().name("InstanceId").value(instanceId).build()).build();
        MetricStat stat = MetricStat.builder().metric(metric).period(300).stat(Statistic.AVERAGE).build();
        MetricDataQuery query = MetricDataQuery.builder().id("cpu").metricStat(stat).returnData(true).build();
        var response = cloudWatch.getMetricData(GetMetricDataRequest.builder().startTime(start).endTime(end).metricDataQueries(query).build());
        return Map.of("instanceId", instanceId, "metric", "CPUUtilization", "timestamps", response.metricDataResults().isEmpty() ? List.of() : response.metricDataResults().get(0).timestamps(), "values", response.metricDataResults().isEmpty() ? List.of() : response.metricDataResults().get(0).values());
    }

    private Map<String, Object> instanceMap(Instance i) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", i.instanceId());
        m.put("type", i.instanceTypeAsString());
        m.put("state", i.state().nameAsString());
        m.put("privateIp", i.privateIpAddress());
        m.put("publicIp", i.publicIpAddress());
        m.put("launchTime", i.launchTime());
        return m;
    }
}
