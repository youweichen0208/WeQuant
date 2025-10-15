package com.quant.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = Map.of(
            "status", "UP",
            "service", "user-service",
            "timestamp", java.time.LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(status);
    }
}