package com.example.jpaspringboot.service.impl;
import com.example.jpaspringboot.entity.VisualIndicatorData;
import com.example.jpaspringboot.repository.VisualIndicatorRepository;
import com.example.jpaspringboot.service.VisualIndicatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class VisualIndicatorServiceImpl implements VisualIndicatorService {

    private static final int CURRENT_YEAR = 2024;

    @Autowired
    private VisualIndicatorRepository repository;

    @Override
    public List<VisualIndicatorData> getAllCurrentData() {
        return repository.findByYear(CURRENT_YEAR);
    }

    @Override
    @Transactional
    public VisualIndicatorData updateIndicatorValue(String regionName, String indicatorKey, double value) {
        VisualIndicatorData data = repository.findByRegionNameAndIndicatorKeyAndYear(regionName, indicatorKey, CURRENT_YEAR);
        if (data != null) {
            data.setValue(value);
            return repository.save(data);
        } else {
            throw new RuntimeException("指定地区或指标不存在！只允许更新，不允许新增。❌");
        }
    }
}
