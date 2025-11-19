package com.example.jpaspringboot.service.devisual;

import com.example.jpaspringboot.entity.devisual.RegionMultiCarbonMetrics;

public interface RegionMultiCarbonMetricsService {
    RegionMultiCarbonMetrics getByRegionName(String regionName);
}
