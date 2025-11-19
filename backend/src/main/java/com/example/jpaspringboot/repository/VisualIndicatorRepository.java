package com.example.jpaspringboot.repository;
import com.example.jpaspringboot.entity.VisualIndicatorData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisualIndicatorRepository extends JpaRepository<VisualIndicatorData, Long> {
    List<VisualIndicatorData> findByYear(int year);
    VisualIndicatorData findByRegionNameAndIndicatorKeyAndYear(String regionName, String indicatorKey, int year);
}
