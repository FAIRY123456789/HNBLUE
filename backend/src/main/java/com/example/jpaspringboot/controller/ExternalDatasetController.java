package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.service.ExternalDatasetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v2/external-datasets")
public class ExternalDatasetController {

    private final ExternalDatasetService externalDatasetService;

    public ExternalDatasetController(ExternalDatasetService externalDatasetService) {
        this.externalDatasetService = externalDatasetService;
    }

    @GetMapping
    public Map<String, Object> datasets() {
        return externalDatasetService.datasets();
    }

    @GetMapping("/{datasetId}/tables")
    public Map<String, Object> tables(@PathVariable String datasetId) {
        return externalDatasetService.tables(datasetId);
    }

    @GetMapping("/{datasetId}/schema")
    public Map<String, Object> schema(
            @PathVariable String datasetId,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return externalDatasetService.schema(datasetId, tableName, page, size, keyword);
    }

    @GetMapping("/{datasetId}/records")
    public Map<String, Object> records(
            @PathVariable String datasetId,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        return externalDatasetService.records(datasetId, tableName, page, size, keyword, sortField, sortDirection);
    }
}
