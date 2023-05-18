package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class Service01Stack extends Stack {
    public Service01Stack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);

        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder
                .create(this, "ALB01")
                .serviceName("service-01")
                .cluster(cluster)
                .cpu(512)
                .desiredCount(2)
                .listenerPort(8080)
                .memoryLimitMiB(1024)
                .taskImageOptions(this.createApplicationLoadBalanceTaskImageOptions())
                .publicLoadBalancer(true)
                .build();

        this.createHealthCheck(service01);

        ScalableTaskCount scalableTaskCount = this.createScalableTaskCount(service01);
        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling",
                CpuUtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(60))
                        .scaleInCooldown(Duration.seconds(60))
                        .build()
                );
    }

    private ApplicationLoadBalancedTaskImageOptions createApplicationLoadBalanceTaskImageOptions() {
        return ApplicationLoadBalancedTaskImageOptions.builder()
                .containerName("aws_project01")
                .image(ContainerImage.fromRegistry("georgemmp/curso_aws_project01:1.0.0"))
                .containerPort(8080)
                .logDriver(this.createAwsLogDriver())
                .build();
    }

    private LogDriver createAwsLogDriver() {
        return LogDriver.awsLogs(
                AwsLogDriverProps.builder()
                        .logGroup(this.createLogGroup())
                        .build()
        );
    }

    private LogGroup createLogGroup() {
        return LogGroup.Builder.create(this, "Service01LogGroup")
                .logGroupName("Service01")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private void createHealthCheck(ApplicationLoadBalancedFargateService service) {
        service.getTargetGroup().configureHealthCheck(
                new HealthCheck.Builder()
                        .path("/actuator/health")
                        .port("8080")
                        .healthyHttpCodes("200")
                        .build()
        );
    }

    private ScalableTaskCount createScalableTaskCount(ApplicationLoadBalancedFargateService service) {
        return service.getService().autoScaleTaskCount(
                EnableScalingProps.builder()
                        .minCapacity(2)
                        .maxCapacity(4)
                        .build()
        );
    }
}
