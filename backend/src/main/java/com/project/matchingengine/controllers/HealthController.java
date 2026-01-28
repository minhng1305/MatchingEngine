package com.project.matchingengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check endpoint for monitoring and load balancers
 * This endpoint should be publicly accessible (no authentication required)
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "service", "matching-engine"
        ));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        // Add readiness checks here (database, Kafka, Redis connectivity)
        return ResponseEntity.ok(Map.of(
            "status", "READY",
            "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        // Simple liveness check - just verify the application is running
        return ResponseEntity.ok(Map.of(
            "status", "ALIVE",
            "timestamp", Instant.now().toString()
        ));
    }
}
