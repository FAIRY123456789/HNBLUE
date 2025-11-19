package com.example.jpaspringboot.repository.devisual;

import com.example.jpaspringboot.entity.devisual.RegionFluxFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionFluxFactorRepository extends JpaRepository<RegionFluxFactor, Integer> {
    List<RegionFluxFactor> findByRegionName(String regionName);
}
