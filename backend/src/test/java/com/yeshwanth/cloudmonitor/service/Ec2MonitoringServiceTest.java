package com.yeshwanth.cloudmonitor.service;

import com.yeshwanth.cloudmonitor.config.AwsProperties;
import com.yeshwanth.cloudmonitor.dto.InstanceSummary;
import com.yeshwanth.cloudmonitor.dto.MetricPoint;
import com.yeshwanth.cloudmonitor.dto.MetricSeries;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Ec2MonitoringServiceTest {

    private static final AwsProperties DEMO = new AwsProperties("ap-south-1", true);
    private static final AwsProperties LIVE = new AwsProperties("ap-south-1", false);

    private Ec2MonitoringService service(AwsProperties properties, Ec2Client ec2, CloudWatchClient cloudWatch) {
        return new Ec2MonitoringService(ec2, cloudWatch, properties, new DemoData());
    }

    @Test
    @DisplayName("demo mode lists instances without touching AWS")
    void demoModeServesGeneratedInstances() {
        Ec2MonitoringService subject = service(DEMO, null, null);

        List<InstanceSummary> instances = subject.instances();

        assertThat(instances).hasSize(3);
        assertThat(instances).extracting(InstanceSummary::state).contains("running", "stopped");
    }

    @Test
    @DisplayName("demo mode returns a CPU series for running instances and none for stopped ones")
    void demoModeCpuSeries() {
        Ec2MonitoringService subject = service(DEMO, null, null);
        String runningId = subject.instances().get(0).instanceId();
        String stoppedId = subject.instances().stream()
                .filter(instance -> "stopped".equals(instance.state()))
                .map(InstanceSummary::instanceId)
                .findFirst()
                .orElseThrow();

        MetricSeries running = subject.cpuHistory(runningId, 3);
        MetricSeries stopped = subject.cpuHistory(stoppedId, 3);

        assertThat(running.source()).isEqualTo(MetricSeries.Source.DEMO);
        assertThat(running.points()).hasSize(36);
        assertThat(running.points()).allSatisfy(point -> {
            assertThat(point.value()).isBetween(0.0, 100.0);
            assertThat(point.timestamp()).isBefore(Instant.now().plusSeconds(60));
        });
        assertThat(stopped.points()).isEmpty();
    }

    @Test
    @DisplayName("the aggregation period grows with the requested window")
    void periodScalesWithWindow() {
        assertThat(Ec2MonitoringService.periodFor(1)).isEqualTo(60);
        assertThat(Ec2MonitoringService.periodFor(3)).isEqualTo(300);
        assertThat(Ec2MonitoringService.periodFor(24)).isEqualTo(900);
    }

    @Test
    @DisplayName("live mode maps the EC2 response into summaries using the Name tag")
    void liveModeMapsInstances() {
        Ec2Client ec2 = mock(Ec2Client.class);
        Instance instance = Instance.builder()
                .instanceId("i-0abcdef1234567890")
                .instanceType("t3.micro")
                .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
                .tags(Tag.builder().key("Name").value("prod-api").build())
                .privateIpAddress("10.0.1.42")
                .launchTime(Instant.parse("2026-01-02T03:04:05Z"))
                .build();
        when(ec2.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(DescribeInstancesResponse.builder()
                        .reservations(Reservation.builder().instances(instance).build())
                        .build());

        List<InstanceSummary> instances = service(LIVE, ec2, mock(CloudWatchClient.class)).instances();

        assertThat(instances).singleElement().satisfies(summary -> {
            assertThat(summary.instanceId()).isEqualTo("i-0abcdef1234567890");
            assertThat(summary.name()).isEqualTo("prod-api");
            assertThat(summary.state()).isEqualTo("running");
            assertThat(summary.privateIp()).isEqualTo("10.0.1.42");
        });
    }

    @Test
    @DisplayName("live mode zips CloudWatch timestamps with values")
    void liveModeMapsMetricData() {
        CloudWatchClient cloudWatch = mock(CloudWatchClient.class);
        when(cloudWatch.getMetricData(any(GetMetricDataRequest.class))).thenReturn(
                GetMetricDataResponse.builder()
                        .metricDataResults(MetricDataResult.builder()
                                .timestamps(Instant.parse("2026-01-02T03:00:00Z"), Instant.parse("2026-01-02T03:05:00Z"))
                                .values(12.5, 37.25)
                                .build())
                        .build());

        MetricSeries series = service(LIVE, mock(Ec2Client.class), cloudWatch).cpuHistory("i-0abcdef1234567890", 3);

        assertThat(series.source()).isEqualTo(MetricSeries.Source.AWS);
        assertThat(series.periodSeconds()).isEqualTo(300);
        assertThat(series.points()).containsExactly(
                new MetricPoint(Instant.parse("2026-01-02T03:00:00Z"), 12.5),
                new MetricPoint(Instant.parse("2026-01-02T03:05:00Z"), 37.25));
    }

    @Test
    @DisplayName("an empty CloudWatch result becomes an empty series, not an error")
    void liveModeHandlesMissingData() {
        CloudWatchClient cloudWatch = mock(CloudWatchClient.class);
        when(cloudWatch.getMetricData(any(GetMetricDataRequest.class)))
                .thenReturn(GetMetricDataResponse.builder().build());

        MetricSeries series = service(LIVE, mock(Ec2Client.class), cloudWatch).cpuHistory("i-0abcdef1234567890", 3);

        assertThat(series.points()).isEmpty();
    }
}
