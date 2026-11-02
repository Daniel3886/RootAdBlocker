package com.android.dnssinkhole.config;

import java.time.Duration;

public final class TestProperties {

    private TestProperties() {
    }

    public static SinkholeProperties withBlocklistDir(String dir) {
        return new SinkholeProperties(
                5300,
                "127.0.0.1",
                Duration.ofSeconds(1),
                1,
                10,
                new SinkholeProperties.Blocklist(dir),
                new SinkholeProperties.QueryLogSettings(7, 100));
    }

    public static SinkholeProperties defaults() {
        return withBlocklistDir("blocklist");
    }
}
