package com.yeshwanth.cloudmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS related settings, bound from the {@code app.aws.*} keys in {@code application.yml}.
 *
 * @param region   AWS region used by the EC2 and CloudWatch clients.
 * @param demoMode when {@code true} the monitoring endpoints serve generated data instead of
 *                 calling AWS, so the dashboard can run without credentials.
 */
@ConfigurationProperties(prefix = "app.aws")
public record AwsProperties(String region, boolean demoMode) {

    private static final String DEFAULT_REGION = "ap-south-1";

    public AwsProperties {
        region = (region == null || region.isBlank()) ? DEFAULT_REGION : region;
    }
}
