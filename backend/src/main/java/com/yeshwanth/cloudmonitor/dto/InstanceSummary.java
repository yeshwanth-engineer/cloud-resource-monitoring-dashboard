package com.yeshwanth.cloudmonitor.dto;

import software.amazon.awssdk.services.ec2.model.Instance;

import java.time.Instant;

/**
 * One EC2 instance as the dashboard needs it.
 *
 * @param instanceId   EC2 instance id, e.g. {@code i-0f1e2d3c4b5a69788}
 * @param name         value of the {@code Name} tag, falling back to the instance id
 * @param instanceType instance type, e.g. {@code t3.micro}
 * @param state        lifecycle state ({@code running}, {@code stopped}, ...)
 * @param privateIp    primary private IPv4 address, {@code null} when not assigned
 * @param publicIp     primary public IPv4 address, {@code null} when not assigned
 * @param launchTime   when the instance was launched
 */
public record InstanceSummary(
        String instanceId,
        String name,
        String instanceType,
        String state,
        String privateIp,
        String publicIp,
        Instant launchTime) {

    /** Name tag is optional on EC2, so the id doubles as the display name. */
    public static InstanceSummary from(Instance instance) {
        String name = instance.tags().stream()
                .filter(tag -> "Name".equals(tag.key()))
                .map(tag -> tag.value())
                .findFirst()
                .orElse(instance.instanceId());
        String state = instance.state() == null ? "unknown" : instance.state().nameAsString();
        return new InstanceSummary(
                instance.instanceId(),
                name,
                instance.instanceTypeAsString(),
                state,
                instance.privateIpAddress(),
                instance.publicIpAddress(),
                instance.launchTime());
    }
}
