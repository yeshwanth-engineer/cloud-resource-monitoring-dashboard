package com.yeshwanth.cloudmonitor.controller;

import com.yeshwanth.cloudmonitor.config.AwsProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liveness probe for the dashboard and for container health checks; it also tells the UI which
 * region and mode the API is running in. {@code /actuator/health} is the readiness probe, since
 * it covers the framework's own health indicators.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final AwsProperties awsProperties;

    public HealthController(AwsProperties awsProperties) {
        this.awsProperties = awsProperties;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "cloud-monitor-backend");
        body.put("region", awsProperties.region());
        body.put("demoMode", awsProperties.demoMode());
        return body;
    }
}
