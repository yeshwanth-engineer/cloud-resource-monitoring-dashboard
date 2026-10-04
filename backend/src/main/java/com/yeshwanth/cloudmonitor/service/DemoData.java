package com.yeshwanth.cloudmonitor.service;

import com.yeshwanth.cloudmonitor.dto.InstanceSummary;
import com.yeshwanth.cloudmonitor.dto.MetricPoint;
import com.yeshwanth.cloudmonitor.dto.MetricSeries;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic stand-in for AWS, used while {@code app.aws.demo-mode=true}.
 *
 * <p>It keeps the API contract, the dashboard, and the tests runnable without AWS credentials.
 * Values are derived from the instance id, so refreshing the page does not redraw a different chart.
 */
@Component
public class DemoData {

    private static final int MIN_POINTS = 6;
    private static final int MAX_POINTS = 2000;
    private static final String CPU_METRIC = "CPUUtilization";

    private record DemoInstance(String id, String name, String type, String state, String privateIp,
                                String publicIp, long launchedDaysAgo, int baseLoad) {
    }

    private static final List<DemoInstance> INSTANCES = List.of(
            new DemoInstance("i-0a1b2c3d4e5f60710", "api-server", "t3.micro", "running", "10.0.1.10", "54.10.10.10", 18, 14),
            new DemoInstance("i-0a1b2c3d4e5f60711", "worker-node", "t3.small", "running", "10.0.1.11", "54.10.10.11", 11, 55),
            new DemoInstance("i-0a1b2c3d4e5f60712", "staging-server", "t3.micro", "stopped", "10.0.1.12", "54.10.10.12", 30, 4));

    public List<InstanceSummary> instances() {
        return INSTANCES.stream()
                .map(instance -> new InstanceSummary(
                        instance.id(),
                        instance.name(),
                        instance.type(),
                        instance.state(),
                        instance.privateIp(),
                        instance.publicIp(),
                        Instant.now().minus(instance.launchedDaysAgo(), ChronoUnit.DAYS)))
                .toList();
    }

    public MetricSeries cpuHistory(String instanceId, int hours, int periodSeconds) {
        DemoInstance instance = find(instanceId);

        // A stopped instance reports no utilization at all, exactly like CloudWatch does.
        if (instance != null && !"running".equals(instance.state())) {
            return MetricSeries.empty(instanceId, CPU_METRIC, "percent", MetricSeries.Source.DEMO, periodSeconds);
        }

        int baseLoad = (instance != null) ? instance.baseLoad() : new Random(instanceId.hashCode()).nextInt(15, 70);
        int pointCount = pointCount(hours, periodSeconds);
        Random random = new Random(instanceId.hashCode());
        Instant end = Instant.now().truncatedTo(ChronoUnit.MINUTES);

        List<MetricPoint> points = new ArrayList<>(pointCount);
        for (int index = pointCount - 1; index >= 0; index--) {
            int step = pointCount - index;
            double wave = 14 * Math.sin(step * 0.55) + 6 * Math.sin(step * 0.17);
            double value = clamp(baseLoad + wave + random.nextDouble(6));
            points.add(new MetricPoint(end.minus((long) index * periodSeconds, ChronoUnit.SECONDS), round(value)));
        }
        return new MetricSeries(instanceId, CPU_METRIC, "percent", MetricSeries.Source.DEMO,
                periodSeconds, List.copyOf(points));
    }

    /** Enough points to cover the requested window at the chosen resolution, within sane bounds. */
    private static int pointCount(int hours, int periodSeconds) {
        int points = (int) (hours * 3600L / periodSeconds);
        return Math.max(MIN_POINTS, Math.min(MAX_POINTS, points));
    }

    private static DemoInstance find(String instanceId) {
        return INSTANCES.stream()
                .filter(candidate -> candidate.id().equals(instanceId))
                .findFirst()
                .orElse(null);
    }

    private static double clamp(double value) {
        return Math.max(2, Math.min(97, value));
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
