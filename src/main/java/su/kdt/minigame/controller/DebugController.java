package su.kdt.minigame.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/_debug")
public class DebugController {

    private static final String BUILD_HASH = System.getenv().getOrDefault("GIT_COMMIT", "local-dev");
    private static final String BUILD_TIMESTAMP = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

    @GetMapping("/build")
    public Map<String, Object> getBuildInfo() {
        return Map.of(
                "build", BUILD_HASH + "-" + BUILD_TIMESTAMP,
                "commit", BUILD_HASH,
                "timestamp", BUILD_TIMESTAMP,
                "env", System.getProperty("spring.profiles.active", "default"),
                "jvm", System.getProperty("java.version")
        );
    }

    @GetMapping("/health")
    public Map<String, Object> getHealthInfo() {
        return Map.of(
                "status", "UP",
                "port", 8080,
                "build", BUILD_HASH + "-" + BUILD_TIMESTAMP,
                "uptime", System.currentTimeMillis()
        );
    }
}