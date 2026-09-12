package com.medilink.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Root Controller for MediLink 2.0 Backend REST API.
 * Provides a health and discovery payload for API clients,
 * and transparently serves the SPA web application to browser requests.
 */
@RestController
@CrossOrigin(origins = "*")
public class RootController {

    @GetMapping(value = {"", "/"})
    public ResponseEntity<Map<String, Object>> rootEndpoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            response.sendRedirect("/index.html");
            return null;
        }

        return ResponseEntity.ok(buildDiscoveryPayload());
    }

    @GetMapping("/forgot-password")
    public void forgotPasswordEndpoint(HttpServletResponse response) throws IOException {
        response.sendRedirect("/forgot-password.html");
    }

    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> apiDiscovery() {
        return ResponseEntity.ok(buildDiscoveryPayload());
    }

    private Map<String, Object> buildDiscoveryPayload() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "MediLink 2.0 - Healthcare & Pharmacy Ecosystem API");
        response.put("status", "UP");
        response.put("version", "2.0.0");
        response.put("architecture", "Unified Fullstack & Decoupled Ecosystem API");
        response.put("frontendUrl", "http://localhost:3000");

        Map<String, String> apiEndpoints = new HashMap<>();
        apiEndpoints.put("auth", "/api/auth/login, /api/auth/register");
        apiEndpoints.put("medicines", "/api/medicines, /api/medicines/search");
        apiEndpoints.put("prescriptions", "/api/prescriptions, /api/prescriptions/upload");
        apiEndpoints.put("pharmacies", "/api/pharmacies/stocks, /api/pharmacies/emergency");
        apiEndpoints.put("reminders", "/api/reminders");
        apiEndpoints.put("chat", "/api/chat/messages, /api/chat/send");
        apiEndpoints.put("ai", "/api/ai/chat, /api/ai/status");
        apiEndpoints.put("eventStream", "/api/events/stream");
        apiEndpoints.put("stats", "/api/stats");
        apiEndpoints.put("admin", "/api/admin/telemetry, /api/admin/users, /api/admin/medicines");

        response.put("endpoints", apiEndpoints);
        return response;
    }
}
