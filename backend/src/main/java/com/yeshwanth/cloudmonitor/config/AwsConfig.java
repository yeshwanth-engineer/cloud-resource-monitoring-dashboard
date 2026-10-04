package com.yeshwanth.cloudmonitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.function.Supplier;

/**
 * AWS SDK clients for the monitoring service.
 *
 * <p>In demo mode a fixed dummy credential provider is used, because the SDK resolves credentials
 * while the client is being built. Without that, starting the app without AWS credentials would
 * fail before it could serve a single generated response.
 *
 * <p>Outside demo mode the default provider chain applies, so credentials come from environment
 * variables, {@code ~/.aws/credentials}, or an IAM instance profile. Never put a key in this file.
 */
@Configuration
public class AwsConfig {

    @Bean
    Ec2Client ec2Client(AwsProperties properties) {
        return build(() -> Ec2Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider(properties))
                .build(), "EC2", properties);
    }

    @Bean
    CloudWatchClient cloudWatchClient(AwsProperties properties) {
        return build(() -> CloudWatchClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider(properties))
                .build(), "CloudWatch", properties);
    }

    private AwsCredentialsProvider credentialsProvider(AwsProperties properties) {
        if (properties.demoMode()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create("demo-key", "demo-secret"));
        }
        return DefaultCredentialsProvider.builder().build();
    }

    private <T> T build(Supplier<T> clientSupplier, String serviceName, AwsProperties properties) {
        try {
            return clientSupplier.get();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not create the " + serviceName + " client for region '"
                    + properties.region() + "'. Supply AWS credentials (or an IAM role) and check AWS_REGION, "
                    + "or keep app.aws.demo-mode=true to run without AWS. Cause: " + e.getMessage(), e);
        }
    }
}
