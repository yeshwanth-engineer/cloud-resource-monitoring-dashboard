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
    private final boolean demo = Boolean.parseBoolean(System.getenv().getOrDefault("DEMO_MODE", "true"));

    public Ec2MonitoringService(Ec2Client ec2, CloudWatchClient cloudWatch) {
        this.ec2 = ec2;
        this.cloudWatch = cloudWatch;
    }

    public List<Map<String, Object>> instances() {
        if (demo) return demoInstances();
        var response = ec2.describeInstances(DescribeInstancesRequest.builder().build());
        List<Map<String, Object>> result = new ArrayList<>();
        response.reservations().forEach(r -> r.instances().forEach(i -> result.add(instanceMap(i))));
        return result;
    }

    public Map<String, Object> cpu(String instanceId, int hours) {
        if (demo) return demoCpu(instanceId, hours);
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        Metric metric = Metric.builder().namespace("AWS/EC2").metricName("CPUUtilization")
                .dimensions(Dimension.builder().name("InstanceId").value(instanceId).build()).build();
        MetricStat stat = MetricStat.builder().metric(metric).period(300).stat(Statistic.AVERAGE).build();
        MetricDataQuery query = MetricDataQuery.builder().id("cpu").metricStat(stat).returnData(true).build();
        var response = cloudWatch.getMetricData(GetMetricDataRequest.builder().startTime(start).endTime(end).metricDataQueries(query).build());
        return Map.of("instanceId", instanceId, "metric", "CPUUtilization", "timestamps", response.metricDataResults().isEmpty() ? List.of() : response.metricDataResults().get(0).timestamps(), "values", response.metricDataResults().isEmpty() ? List.of() : response.metricDataResults().get(0).values());
    }

    private List<Map<String, Object>> demoInstances() {
        return List.of(
            Map.of("id","i-demo-001","name","api-server","type","t3.micro","state","running","privateIp","10.0.1.10","publicIp","54.10.10.10","launchTime",Instant.now().minus(18,ChronoUnit.DAYS)),
            Map.of("id","i-demo-002","name","worker-node","type","t3.small","state","running","privateIp","10.0.1.11","publicIp","54.10.10.11","launchTime",Instant.now().minus(11,ChronoUnit.DAYS)),
            Map.of("id","i-demo-003","name","staging-server","type","t3.micro","state","stopped","privateIp","10.0.1.12","publicIp","54.10.10.12","launchTime",Instant.now().minus(30,ChronoUnit.DAYS))
        );
    }

    private Map<String, Object> demoCpu(String instanceId, int hours) {
        int points = Math.max(6, Math.min(hours * 12, 36));
        List<String> timestamps = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        Instant end = Instant.now();
        int seed = Math.abs(instanceId.hashCode() % 17);
        for (int i = points - 1; i >= 0; i--) {
            timestamps.add(end.minus(i * 5L, ChronoUnit.MINUTES).toString());
            double value = 25 + seed + 12 * Math.sin((points - i) * 0.55) + (i % 5) * 1.7;
            values.add(Math.round(Math.max(4, Math.min(92, value)) * 100.0) / 100.0);
        }
        return Map.of("instanceId", instanceId, "metric", "CPUUtilization", "timestamps", timestamps, "values", values, "source", "DEMO_MODE");
    }

    private Map<String, Object> instanceMap(Instance i) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", i.instanceId());
        m.put("name", i.tags().stream().filter(t -> "Name".equals(t.key())).map(t -> t.value()).findFirst().orElse(i.instanceId()));
        m.put("type", i.instanceTypeAsString());
        m.put("state", i.state().nameAsString());
        m.put("privateIp", i.privateIpAddress());
        m.put("publicIp", i.publicIpAddress());
        m.put("launchTime", i.launchTime());
        return m;
    }
}
