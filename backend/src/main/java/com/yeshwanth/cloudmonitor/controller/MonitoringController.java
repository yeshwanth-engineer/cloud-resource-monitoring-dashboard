package com.yeshwanth.cloudmonitor.controller;

import com.yeshwanth.cloudmonitor.service.Ec2MonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    private final Ec2MonitoringService service;

    public MonitoringController(Ec2MonitoringService service) { this.service = service; }

    @GetMapping("/instances")
    public List<Map<String, Object>> instances() { return service.instances(); }

    @GetMapping("/instances/{instanceId}/cpu")
    public Map<String, Object> cpu(@PathVariable String instanceId, @RequestParam(defaultValue = "3") int hours) {
        if (hours < 1 || hours > 24) throw new IllegalArgumentException("hours must be between 1 and 24");
        return service.cpu(instanceId, hours);
    }
}
