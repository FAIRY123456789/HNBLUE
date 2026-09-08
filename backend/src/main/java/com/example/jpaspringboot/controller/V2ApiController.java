package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.service.V2DataService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin
public class V2ApiController {

    private final V2DataService v2DataService;

    public V2ApiController(V2DataService v2DataService) {
        this.v2DataService = v2DataService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return v2DataService.health();
    }

    @GetMapping("/dashboard/summary")
    public Map<String, Object> dashboardSummary() {
        return v2DataService.dashboardSummary();
    }

    @GetMapping("/sources")
    public Map<String, Object> sources(@RequestParam Map<String, String> filters) {
        return v2DataService.sources(filters);
    }

    @GetMapping("/mangrove-cover")
    public Map<String, Object> mangroveCover(@RequestParam Map<String, String> filters) {
        return v2DataService.mangroveCover(filters);
    }

    @GetMapping("/region-metrics")
    public Map<String, Object> regionMetrics(@RequestParam Map<String, String> filters) {
        return v2DataService.regionMetrics(filters);
    }

    @GetMapping("/literature-carbon")
    public Map<String, Object> literatureCarbon(@RequestParam Map<String, String> filters) {
        return v2DataService.literatureCarbon(filters);
    }

    @GetMapping("/region-overview/{regionId}")
    public Map<String, Object> regionOverview(@PathVariable String regionId) {
        return v2DataService.regionOverview(regionId);
    }

    @GetMapping("/ai/context")
    public Map<String, Object> aiContext(@RequestParam Map<String, String> filters) {
        return v2DataService.aiContext(filters);
    }

    @GetMapping("/optional/status")
    public Map<String, Object> optionalStatus() {
        return v2DataService.optionalStatus();
    }
}
