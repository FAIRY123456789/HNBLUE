package com.example.jpaspringboot.service;
import com.example.jpaspringboot.entity.VisualIndicatorData;

import java.util.List;

public interface VisualIndicatorService {
    List<VisualIndicatorData> getAllCurrentData();
    VisualIndicatorData updateIndicatorValue(String regionName, String indicatorKey, double value);
}
