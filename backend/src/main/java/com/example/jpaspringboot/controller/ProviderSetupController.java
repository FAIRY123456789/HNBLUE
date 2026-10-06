package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.service.ai.ApiSecretService;
import com.example.jpaspringboot.service.impl.UserActivityLogServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/provider-setup")
public class ProviderSetupController {
    private final ApiSecretService apiSecretService;
    private final UserActivityLogServiceImpl activityLogService;

    public ProviderSetupController(ApiSecretService apiSecretService, UserActivityLogServiceImpl activityLogService) {
        this.apiSecretService = apiSecretService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public ApiSecretService.ProviderSetupStatus status() {
        return apiSecretService.setupStatus();
    }

    @PostMapping
    public ResponseEntity<?> initialize(@RequestBody ApiKeyRequest body, HttpServletRequest request) {
        if (!isSecureCredentialTransport(request)) {
            return ResponseEntity.status(HttpStatus.UPGRADE_REQUIRED).body(Map.of(
                    "message", "API Key 只能通过 HTTPS、本机回环地址或 SSH 加密隧道设置"
            ));
        }
        try {
            ApiSecretService.ProviderSetupStatus status = apiSecretService.saveDeepSeekApiKeyOnce(body == null ? null : body.apiKey());
            activityLogService.record(null, "one-time-setup", "System", null, "INITIALIZE_AI_SECRET",
                    "One-time DeepSeek credential initialized and permanently locked",
                    "/api/internal/provider-setup", "SUCCESS", request.getRemoteAddr());
            return ResponseEntity.ok(status);
        } catch (ApiSecretService.SecretAlreadyConfiguredException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage(), "locked", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", ex.getMessage()));
        }
    }

    private boolean isSecureCredentialTransport(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && "https".equalsIgnoreCase(forwardedProto.split(",", 2)[0].trim())) return true;
        if (forwardedProto == null && request.isSecure()) return true;
        String clientAddress = request.getHeader("X-Real-IP");
        if (clientAddress == null || clientAddress.isBlank()) clientAddress = request.getRemoteAddr();
        String normalized = clientAddress == null ? "" : clientAddress.trim();
        return "127.0.0.1".equals(normalized) || "::1".equals(normalized) || "0:0:0:0:0:0:0:1".equals(normalized);
    }

    public record ApiKeyRequest(String apiKey) {}
}
