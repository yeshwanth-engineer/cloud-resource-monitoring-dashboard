package com.yeshwanth.cloudmonitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Configuration
public class AwsConfig {
    @Bean
    Ec2Client ec2Client() {
        return Ec2Client.builder().region(Region.of(System.getenv().getOrDefault("AWS_REGION", "ap-south-1"))).build();
    }

    @Bean
    CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder().region(Region.of(System.getenv().getOrDefault("AWS_REGION", "ap-south-1"))).build();
    }
}
