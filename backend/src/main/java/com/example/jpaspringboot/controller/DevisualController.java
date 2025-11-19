/**
 * 区域生态数据可视化API控制器
 *
 * 功能概述：
 * • 提供海南省各区域生态碳汇数据的综合查询接口
 * • 集成多维度生态指标数据（碳储量、碳通量、物种组成等）
 * • 支持前端可视化图表的数据供给
 *
 * 数据维度：
 * ① 区域基础信息概览
 * ② 历年碳储与碳通量趋势
 * ③ 碳通量组成结构分析
 * ④ 多碳指标对比分析（雷达图+柱状图）
 * ⑤ 长期碳储变化趋势
 * ⑥ 生态功能区划地图数据
 * ⑦ 通量影响因子相关性分析
 * ⑧ 优势物种组成分布
 * ⑨ 经济价值估算预测
 *
 * 区域覆盖：
 * 海南省18个市县（琼海、文昌、万宁、海口、三亚、儋州、五指山、东方、
 * 定安县、屯昌县、澄迈县、临高县、白沙、昌江、乐东、陵水、保亭、琼中）
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.entity.devisual.*;
import com.example.jpaspringboot.service.devisual.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/devisual")
@CrossOrigin
public class DevisualController {

    // 区域数据服务依赖注入
    @Autowired private RegionSummaryInfoService summaryInfoService;
    @Autowired private RegionYearlyCarbonService yearlyCarbonService;
    @Autowired private RegionFluxCompositionService fluxCompositionService;
    @Autowired private RegionMultiCarbonMetricsService multiCarbonMetricsService;
    @Autowired private RegionYearlyTrendService yearlyTrendService;
    @Autowired private RegionZoneMapService zoneMapService;
    @Autowired private RegionFluxFactorService fluxFactorService;
    @Autowired private RegionSpeciesCompositionService speciesCompositionService;
    @Autowired private RegionEconomyProjectionService economyProjectionService;

    /**
     * 获取指定区域全维度生态数据
     *
     * @param regionName 区域名称（海南省18个市县之一）
     * @return 包含9个维度的区域生态数据集合
     * @throws IllegalArgumentException 当区域名称不合法时抛出
     */
    @GetMapping("/{regionName}")
    public Map<String, Object> getRegionAllData(@PathVariable String regionName) {
        // 区域名称合法性校验：仅允许海南省18个指定市县名称
        Set<String> validRegionNames = new HashSet<>(Arrays.asList(
                "琼海", "文昌", "万宁", "海口", "三亚", "儋州", "五指山", "东方",
                "定安县", "屯昌县", "澄迈县", "临高县", "白沙", "昌江", "乐东",
                "陵水", "保亭", "琼中"
        ));

        if (regionName == null || !validRegionNames.contains(regionName)) {
            throw new IllegalArgumentException("Invalid region name: " + regionName);
        }

        Map<String, Object> result = new HashMap<>();

        // ① 区域基础信息概览（面积、人口、生态类型等）
        RegionSummaryInfo summary = summaryInfoService.getByRegionName(regionName);
        result.put("regionInfo", summary);

        // ② 历年碳储与碳通量数据（时间序列趋势）
        List<RegionYearlyCarbon> carbonList = yearlyCarbonService.getByRegionName(regionName);
        result.put("carbonTrends", carbonList);

        // ③ 碳通量组成结构（光合作用、呼吸作用、净交换等）
        List<RegionFluxComposition> fluxList = fluxCompositionService.getByRegionName(regionName);
        result.put("fluxComposition", fluxList);

        // ④ 多碳指标对比分析（碳密度、碳储量、碳通量等多维度指标）
        RegionMultiCarbonMetrics metrics = multiCarbonMetricsService.getByRegionName(regionName);
        result.put("multiCarbonMetrics", metrics);

        // ⑤ 多年碳储变化趋势（线性/非线性趋势分析）
        List<RegionYearlyTrend> trendList = yearlyTrendService.getByRegionName(regionName);
        result.put("yearlyTrends", trendList);

        // ⑥ 生态功能区划地图数据（核心区、缓冲区、实验区等）
        RegionZoneMap zoneMap = zoneMapService.getByRegionName(regionName);
        result.put("ecoZones", zoneMap);

        // ⑦ 通量影响因子分析（温度、降水、土壤等影响因素）
        List<RegionFluxFactor> fluxFactors = fluxFactorService.getByRegionName(regionName);
        result.put("fluxSamples", fluxFactors);

        // ⑧ 优势物种组成分布（红树林、热带雨林等物种占比）
        List<RegionSpeciesComposition> species = speciesCompositionService.getByRegionName(regionName);
        result.put("speciesPie", species);

        // ⑨ 经济价值估算预测（碳交易价值、生态服务价值等）
        List<RegionEconomyProjection> economy = economyProjectionService.getByRegionName(regionName);
        result.put("economyProjections", economy);

        return result;
    }

    /**
     * 区域名称参数异常处理
     *
     * @param ex 非法参数异常
     * @return 标准错误响应（HTTP 400状态码）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        return errorResponse;
    }
}