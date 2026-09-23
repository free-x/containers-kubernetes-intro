package de.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class HealthController {

    private final AtomicBoolean healthy = new AtomicBoolean(true);

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean isHealthy = healthy.get();
        HttpStatus status = isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(Map.of("status", isHealthy ? "UP" : "DOWN"));
    }

    @PostMapping("/admin/health/toggle")
    public Map<String, String> toggleHealth() {
        boolean newValue = !healthy.get();
        healthy.set(newValue);
        return Map.of("status", newValue ? "UP" : "DOWN");
    }

    @PostMapping("/admin/restart")
    public ResponseEntity<Map<String, String>> restart() {
        Thread shutdown = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.exit(1);
        });
        shutdown.setDaemon(true);
        shutdown.start();
        return ResponseEntity.accepted().body(Map.of("status", "restarting"));
    }
}
