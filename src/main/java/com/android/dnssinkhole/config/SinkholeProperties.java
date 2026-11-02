package com.android.dnssinkhole.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "sinkhole")
public record SinkholeProperties(
        @DefaultValue("5300") @Min(0) @Max(65535) int listenPort,
        @DefaultValue("1.1.1.1") @NotBlank String upstreamDns,
        @DefaultValue("3s") Duration upstreamTimeout,
        @DefaultValue("4") @Min(1) int workerThreads,
        @DefaultValue("1000") @Min(1) int queueCapacity,
        @DefaultValue @Valid Blocklist blocklist,
        @DefaultValue @Valid QueryLogSettings queryLog) {

    public record Blocklist(@DefaultValue("blocklist") @NotBlank String dir) {
    }

    public record QueryLogSettings(
            @DefaultValue("7") @Min(0) int retentionDays,
            @DefaultValue("10000") @Min(1) int bufferCapacity) {
    }
}
