package com.example.jpaspringboot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/model")
public class ModelProxyController {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${model.api.url:http://localhost:8880}")
    private String modelApiUrl;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            Object result = restTemplate.getForObject(modelApiUrl + "/ping", Object.class);
            return ResponseEntity.ok(Map.of("ok", true, "message", "模型服务已连接", "upstream", result == null ? "pong" : result));
        } catch (Exception ex) {
            return ResponseEntity.ok(Map.of("ok", false, "message", "模型服务当前不可用，参数仍可编辑；连接恢复后可提交估算"));
        }
    }

    @PostMapping("/predict-carbon")
    public ResponseEntity<?> predict(@RequestBody Map<String, Object> payload) {
        try {
            Object result = restTemplate.postForObject(modelApiUrl + "/api/predict-carbon", payload, Object.class);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "模型服务当前不可用", "error", ex.getClass().getSimpleName()));
        }
    }

    @PostMapping("/predict-carbon/batch")
    public ResponseEntity<?> batch(@RequestBody List<Map<String, Object>> payload) {
        try {
            Object result = restTemplate.postForObject(modelApiUrl + "/api/predict-carbon/batch", payload, Object.class);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", "模型服务当前不可用", "error", ex.getClass().getSimpleName()));
        }
    }
}
