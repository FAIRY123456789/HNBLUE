package com.example.jpaspringboot.repository.devisual;
import com.example.jpaspringboot.entity.devisual.RegionSummaryInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionSummaryInfoRepository extends JpaRepository<RegionSummaryInfo, Integer> {
    RegionSummaryInfo findByRegionName(String regionName);
}
