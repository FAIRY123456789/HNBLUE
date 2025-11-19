package com.example.jpaspringboot.controller;
import com.example.jpaspringboot.entity.VisualIndicatorData;
import com.example.jpaspringboot.service.VisualIndicatorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/indicators")
public class VisualIndicatorController {

    @Autowired
    private VisualIndicatorService service;

    // ✅ 查询所有当前年份数据
    @GetMapping("/current")
    public ResponseEntity<List<VisualIndicatorData>> getCurrentData() {
        return ResponseEntity.ok(service.getAllCurrentData());
    }

    // ✅ 更新指定地区某一指标值（无需校验 Token）
    @PutMapping("/update")
    public ResponseEntity<?> updateIndicator(@RequestParam String region,
                                             @RequestParam String indicator,
                                             @RequestParam double value) {
        try {
            VisualIndicatorData updated = service.updateIndicatorValue(region, indicator, value);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新失败: " + e.getMessage());
        }
    }
}