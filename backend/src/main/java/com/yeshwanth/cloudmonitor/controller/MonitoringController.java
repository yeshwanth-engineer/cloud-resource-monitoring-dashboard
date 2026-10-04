package com.yeshwanth.cloudmonitor.controller;

import com.yeshwanth.cloudmonitor.dto.InstanceSummary;
import com.yeshwanth.cloudmonitor.dto.MetricSeries;
import com.yeshwanth.cloudmonitor.service.Ec2MonitoringService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Read-only cloud resource endpoints. They require a valid token with the {@code ADMIN} role,
 * which {@link com.yeshwanth.cloudmonitor.security.SecurityConfig} enforces.
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    static final int MIN_HOURS = 1;
    static final int MAX_HOURS = 24;

    /** EC2 instance ids are {@code i-} plus a hex suffix; demo ids may contain letters. */
    private static final Pattern INSTANCE_ID = Pattern.compile("^i-[a-z0-9]{6,32}$");

    private final Ec2MonitoringService monitoringService;

    public MonitoringController(Ec2MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/instances")
    public List<InstanceSummary> instances() {
        return monitoringService.instances();
    }

    @GetMapping("/instances/{instanceId}/cpu")
    public MetricSeries cpuHistory(@PathVariable String instanceId,
                                  @RequestParam(name = "hours", defaultValue = "3") int hours) {
        if (!INSTANCE_ID.matcher(instanceId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "instanceId must look like an EC2 instance id (i- followed by 6-32 lowercase letters or digits)");
        }
        if (hours < MIN_HOURS || hours > MAX_HOURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "hours must be between " + MIN_HOURS + " and " + MAX_HOURS);
        }
        return monitoringService.cpuHistory(instanceId, hours);
    }
}
