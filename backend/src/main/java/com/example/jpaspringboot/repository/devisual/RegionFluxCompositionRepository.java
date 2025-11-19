package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionFluxComposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionFluxCompositionRepository extends JpaRepository<RegionFluxComposition, Integer> {
    List<RegionFluxComposition> findByRegionName(String regionName);
}
