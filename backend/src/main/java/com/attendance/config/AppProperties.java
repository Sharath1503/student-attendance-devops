package com.attendance.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of every "app.*" setting (all of them are fed from environment variables). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Attendance attendance, Bootstrap bootstrap, Seed seed) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Attendance(int editWindowDays, double lowThresholdPercent) {}

    public record Bootstrap(String adminUsername, String adminPassword) {}

    public record Seed(boolean demoEnabled, String demoPassword) {}
}
